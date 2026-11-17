package com.aether.assistant.core.agent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Orchestrates Planner → Executor → Verifier → Error Handler with explicit
 * confirmation gates for sensitive / financial / state-changing actions.
 */
class Agent(
    private val planner: Planner,
    private val executor: Executor,
    private val verifier: Verifier,
    private val errorHandler: ErrorHandler
) {
    private val _state = MutableStateFlow(AgentState())
    val state: StateFlow<AgentState> = _state

    private val _confirmation = MutableStateFlow<ConfirmationRequest?>(null)
    val confirmation: StateFlow<ConfirmationRequest?> = _confirmation

    fun cancel() {
        _state.value = AgentState(running = false, messages = _state.value.messages + "Task cancelled.")
        _confirmation.value = null
    }

    fun submit(input: String, extras: Map<String, Any> = emptyMap()) {
        if (_state.value.running) return
        val plan = planner.plan(input, extras)
        _state.value = AgentState(plan = plan, running = true, messages = listOf("▶ ${plan.goal}"))
        runSteps(plan, extras, 0)
    }

    fun approve() {
        val req = _confirmation.value ?: return
        _confirmation.value = null
        req.onApprove()
    }

    fun deny() {
        val req = _confirmation.value ?: return
        _confirmation.value = null
        req.onDeny()
    }

    private fun runSteps(plan: TaskPlan, extras: Map<String, Any>, index: Int) {
        if (index >= plan.steps.size) {
            _state.update { it.copy(running = false, messages = it.messages + "✅ Task complete.") }
            return
        }
        val step = plan.steps[index]
        updateStep(index, StepStatus.RUNNING, "Working…")

        if (step.action is AgentAction.Sensitive) {
            val request = ConfirmationRequest(
                reason = step.action.reason,
                onApprove = {
                    CoroutineScope(Dispatchers.Default).launch {
                        runInner(plan, extras, index, step.action.inner)
                    }
                },
                onDeny = {
                    updateStep(index, StepStatus.SKIPPED, "Skipped by you")
                    _state.update { it.copy(messages = it.messages + "⏭ Skipped: ${step.label}") }
                    runSteps(plan, extras, index + 1)
                }
            )
            _confirmation.value = request
            return
        }

        CoroutineScope(Dispatchers.Default).launch {
            runInner(plan, extras, index, step.action)
        }
    }

    private suspend fun runInner(plan: TaskPlan, extras: Map<String, Any>, index: Int, action: AgentAction) {
        var result = executor.execute(action, extras)
        var check = verifier.verify(action, result)
        var attempt = 0

        while (!check.ok && attempt < 2) {
            val recovery = errorHandler.recover(action, result.exceptionOrNull()
                ?: Throwable(check.detail), attempt)
            attempt++
            if (recovery.recovered) {
                result = Result.success(recovery.detail)
                check = verifier.verify(action, result)
            } else break
        }

        val detail = result.getOrElse { check.detail }
        if (check.ok) {
            updateStep(index, StepStatus.DONE, detail)
            _state.update { it.copy(messages = it.messages + "• $detail") }
        } else {
            updateStep(index, StepStatus.FAILED, detail)
            _state.update {
                it.copy(messages = it.messages + "⚠ $detail", lastError = detail)
            }
        }
        runSteps(plan, extras, index + 1)
    }

    private fun updateStep(index: Int, status: StepStatus, detail: String) {
        _state.update { s ->
            val plan = s.plan ?: return@update s
            val steps = plan.steps.toMutableList()
            steps[index] = steps[index].copy(status = status, detail = detail.take(400))
            s.copy(plan = plan.copy(steps = steps))
        }
    }
}
