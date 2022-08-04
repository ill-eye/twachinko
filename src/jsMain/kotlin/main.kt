import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.appendText
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.WebSocket
import org.w3c.dom.events.MouseEvent
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

val cvs = document.getElementById("cvs") as HTMLCanvasElement
val ctx = cvs.getContext("2d")!! as CanvasRenderingContext2D
const val width = 700
const val height = 600

val circlesWorld = circlesWorld(CirclesContext(DimContext(width.toDouble(), height.toDouble())))
val fieldWorld = fieldWorld(DimContext(width.toDouble(), height.toDouble()))
val pachinkoWorld = pachinkoWorld(DimContext(width.toDouble(), height.toDouble()))
val twoDKinematicWorld = kinematicWorld(TimeContext(0.0, DimContext(width.toDouble(), height.toDouble())))

fun getMousePos(canvas: HTMLCanvasElement, e: MouseEvent): Vector {
    val r = canvas.getBoundingClientRect()
    return Vector(
        e.clientX.toDouble() - r.left,
        e.clientY.toDouble() - r.top
    )
}

var lastMousePos: Vector = Vector((width / 2).toDouble(), (height / 2).toDouble())

fun main() {
    val ws = WebSocket("ws://localhost:8080/dodo")
    ws.onmessage = { ev ->
        document.body!!.appendText(ev.data as? String ?: "bad message")
        circlesWorld.createCircle(
            position = Vector(
                Random.nextDouble(width.toDouble() / 3, width.toDouble() / 3 * 2),
                Random.nextDouble(height.toDouble() / 3, height.toDouble() / 3 * 2)
            ),
            radius = Random.nextDouble(1.0, 40.0),
            velocity = Vector(
                Random.nextDouble(-1.0, 1.0),
                Random.nextDouble(-1.0, 1.0)
            )
        )
    }
    val scale = 1
    cvs.width = width * scale
    cvs.height = height * scale
    init2dKinematicWorld()
    run()
}

private fun init2dKinematicWorld() {
    twoDKinematicWorld.registerSystem(TwoDKinematicSystem())
    twoDKinematicWorld.registerSystem(TwoDRenderSystem(ctx))
    twoDKinematicWorld.createParticle(Vector(0.0, 0.0)) { t -> Vector(t, t) }
    twoDKinematicWorld.createParticle(Vector(0.0, 0.0)) { t -> Vector(t, t.pow(1.1)) }
    twoDKinematicWorld.createParticle(Vector(0.0, 0.0)) { t ->
        Vector(
            100 + sin(t / 10) * 100,
            100 + cos(t / 10) * 100
        )
    }
}

private fun initFieldWorld() {
    val fieldInputSystem = FieldInputSystem()
    cvs.onmousedown = {
        fieldInputSystem.clicked = true
        false
    }
    cvs.onmousemove = { e ->
        fieldInputSystem.mousePos = getMousePos(cvs, e)
        false
    }
    fieldWorld.registerSystem(fieldInputSystem)
    fieldWorld.registerSystem(FieldSystem())
    fieldWorld.registerSystem(FieldRenderSystem(ctx))
    val vpd = 30
    for (x in (0..vpd)) {
        for (y in (0..vpd)) {
            fieldWorld.createVector(Vector((width / vpd * x).toDouble(), (height / vpd * y).toDouble()))
        }
    }
    fieldWorld.createVector(Vector.zero()).also { fieldWorld.tag("player", it) }
}

@Suppress("unused")
private fun initCirclesWorld() {
    cvs.onmousemove = { e ->
        lastMousePos = getMousePos(cvs, e)
        false
    }
    circlesWorld.registerSystem(MovingSystem(width.toDouble(), height.toDouble()))
    circlesWorld.registerSystem(RotatingSystem())
    circlesWorld.registerSystem(CircleRenderSystem(ctx))
    circlesWorld.registerSystem(DebugRenderSystem(ctx))
    circlesWorld.registerSystem(InputSystem { lastMousePos })
    for (i in (0..5)) {
        circlesWorld.createCircle(
            position = Vector(
                width.toDouble() / 8 * (i % 8),
                height.toDouble() / 5 * (i / 5)
            ),
            radius = Random.nextDouble(1.0, 40.0),
            velocity = Vector(
                Random.nextDouble(-1.0, 1.0),
                Random.nextDouble(-1.0, 1.0)
            )
        )
    }
    val player = circlesWorld.createCircle(
        position = lastMousePos,
        radius = 60.0,
        velocity = Vector.zero()
    )
    circlesWorld.tag("player", player)
}

private fun initPachinkoWorld() {
    pachinkoWorld.registerSystem(GravitySystem())
    pachinkoWorld.registerSystem(PachinkoMovingSystem(width.toDouble(), height.toDouble()))
    pachinkoWorld.registerSystem(PachinkoDebugRenderSystem(ctx))
    pachinkoWorld.registerSystem(ForceApplicationSystem())
    pachinkoWorld.registerSystem(PachinkoRenderSystem(ctx))
    for (i in (0..5)) {
        pachinkoWorld.createBall(
            position = Vector(
                width.toDouble() / 8 * (i % 8),
                height.toDouble() / 5 * (i / 5)
            ),
            radius = Random.nextDouble(1.0, 40.0),
            velocity = Vector(
                Random.nextDouble(-1.0, 1.0),
                Random.nextDouble(-1.0, 1.0)
            ),
            mass = 1.0
        )
    }
}

@JsName("run")
fun run() {
    window.setInterval({
        twoDKinematicWorld.tick()
    }, 20)
}