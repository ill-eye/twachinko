import org.w3c.dom.*
import kotlin.math.*
import kotlin.math.sin
import kotlin.random.Random
import kotlin.reflect.KClass

class TimeContext(var t: Double, dimContext: DimContext, var dt: Double = 0.1) : DimContext(
    dimContext.width,
    dimContext.height,
    dimContext.scale
)

data class TwoDKinematicCurve(var f_t: (Double) -> Vector) : Component

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class TwoDKinematicSystem(
) : Component2System<Position, TwoDKinematicCurve, TimeContext>(Position::class, TwoDKinematicCurve::class) {
    override fun doProcessEntity(entity: Int, position: Position, curve: TwoDKinematicCurve) {
//        println("kinemating ${entity}: $position")
        position.v.set(curve.f_t(world.globals.t))
    }

    override fun after() {
        world.globals.t += world.globals.dt
    }
}

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class TwoDRenderSystem(
    private val ctx: CanvasRenderingContext2D
) : Component2System<Position, TwoDKinematicCurve, TimeContext>(Position::class, TwoDKinematicCurve::class) {
    override fun before() {
        ctx.fillStyle = "#ffffff"
      //  ctx.fillRect(0.0, 0.0, world.globals.width, world.globals.height)
    }

    override fun doProcessEntity(entity: Int, position: Position, curve: TwoDKinematicCurve) {
//        println("processing ${entity}")
        ctx.scale(world.globals.scale, world.globals.scale)
        ctx.translate(position.v.x, position.v.y)
        drawRotation(position)
        val dv = (curve.f_t(world.globals.t + world.globals.dt) - position.v) / world.globals.dt
        ctx.beginPath()
        ctx.moveTo(0.0, 0.0)
        ctx.lineCap = CanvasLineCap.BUTT
        ctx.lineTo(dv.x * 10, dv.y * 10)
        ctx.strokeStyle = "#ff3434"
        ctx.stroke()
        ctx.resetTransform()
    }

    private fun drawRotation(position: Position) {
        ctx.beginPath()
        ctx.moveTo(0.0, 0.0)
        ctx.lineCap = CanvasLineCap.ROUND
        (position.r).let {
            ctx.lineTo(it.x, it.y)
            ctx.ellipse(it.x, it.y, 1.0, 1.0, 0.0, 0.0, 2 * PI)
        }
        ctx.strokeStyle = "#20ff20"
        ctx.stroke()
    }
}

fun kinematicWorld(context: TimeContext) = World(object : RegisteredComponents {
    override val components: Map<KClass<out Component>, () -> Component>
        get() = mapOf(
            Position::class to { Position(Vector.zero()) },
            TwoDKinematicCurve::class to { TwoDKinematicCurve { t -> Vector(t, t) } }
        )
}, context)

fun World<TimeContext>.createParticle(position: Vector, twoDCurve: (Double) -> Vector): Int {
    val e = createEntity()
    val pos = addComponent(e, Position::class)
    val curve = addComponent(e, TwoDKinematicCurve::class)
    pos.v.set(position)
    curve.f_t = twoDCurve
    return e
}