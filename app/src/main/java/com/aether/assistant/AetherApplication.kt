package com.aether.assistant

import android.app.Application
import com.aether.assistant.core.agent.Agent
import com.aether.assistant.core.agent.ErrorHandler
import com.aether.assistant.core.agent.Executor
import com.aether.assistant.core.agent.Planner
import com.aether.assistant.core.agent.Verifier
import com.aether.assistant.core.ai.AiManager
import com.aether.assistant.core.ai.LocalEngine
import com.aether.assistant.core.automation.AutomationController
import com.aether.assistant.core.automation.PaymentFlow
import com.aether.assistant.core.code.CodeGenerator
import com.aether.assistant.core.files.FileAnalyzer
import com.aether.assistant.core.memory.MemoryRepository
import com.aether.assistant.core.permissions.PermissionManager
import com.aether.assistant.core.settings.SettingsRepository
import com.aether.assistant.core.vision.CameraAnalyzer
import com.aether.assistant.core.voice.VoiceController
import com.aether.assistant.core.web.WebRepository
import com.aether.assistant.core.youtube.YouTubeUploader

class AetherApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Manual dependency container — no paid/cloud SDK, fully local wiring. */
class AppContainer(app: AetherApplication) {
    /** Session-only OAuth token for YouTube; never persisted or bundled. */
    @Volatile var youtubeToken: String = ""

    val settings = SettingsRepository(app)
    val memory = MemoryRepository(app)
    val permissions = PermissionManager(app)
    val voice = VoiceController(app)
    val localEngine = LocalEngine(app)
    val aiManager = AiManager()
    val automation = AutomationController(app)
    val payment = PaymentFlow(app)
    val web = WebRepository()
    val files = FileAnalyzer(app)
    val code = CodeGenerator()
    val camera = CameraAnalyzer(app)
    val youtube = YouTubeUploader()

    val executor = Executor(
        context = app,
        aiManager = aiManager,
        localEngine = localEngine,
        settings = settings,
        memory = memory,
        automation = automation,
        payment = payment,
        web = web,
        files = files,
        code = code,
        camera = camera,
        youtube = youtube
    )
    val agent = Agent(Planner(), executor, Verifier(), ErrorHandler(aiManager, localEngine))
}
