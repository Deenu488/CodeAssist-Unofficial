package $packagename

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle

import java.util.Random

class GameSuperClass : ApplicationAdapter() {
	private lateinit var font: BitmapFont
	private lateinit var birdCircle: Circle
	private lateinit var randomGenerator: Random
	private lateinit var batch: SpriteBatch
	private lateinit var topTube: Texture
	private lateinit var bottomTube: Texture
	private lateinit var background: Texture
	private lateinit var gameover: Texture

	private var birdY = 0f
	private var distanceBetweenTubes = 0f
	private var maxTubeOffset = 0f
	private var velocity = 0f

	private var flapState = 0
	private var score = 0
	private var scoringTube = 0
	private var gameState = 0
	private var width = 0f
	private var height = 0f

	private val gap = 400f
	private val tubeVelocity = 4f
	private val numberOfTubes = 4
	private val gravity = 2

	private val tubeX = FloatArray(numberOfTubes)
	private val tubeOffset = FloatArray(numberOfTubes)

	private val topTubeRectangles = Array(numberOfTubes) { Rectangle() }
	private val bottomTubeRectangles = Array(numberOfTubes) { Rectangle() }
	private val birds = arrayOfNulls<Texture>(2)

	/* This method is being called when application is created */
	override fun create() {
		batch = SpriteBatch()
		background = Texture("bg.png")
		gameover = Texture("gameover.png")
		birdCircle = Circle()
		font = BitmapFont()
		font.color = Color.WHITE
		font.data.setScale(10f)

		width = Gdx.graphics.width.toFloat()
		height = Gdx.graphics.height.toFloat()

		birds.set(0, Texture("bird.png"))
		birds.set(1, Texture("bird2.png"))

		topTube = Texture("toptube.png")
		bottomTube = Texture("bottomtube.png")
		maxTubeOffset = height / 2 - gap / 2 - 100
		randomGenerator = Random()
		distanceBetweenTubes = width * 3 / 4

		startGame()
	}

	private fun startGame() {
		birdY = height / 2 - birds[0]!!.height / 2

		for (i in 0 until numberOfTubes) {
			tubeOffset[i] = (randomGenerator.nextFloat() - 0.5f) * (height - gap - 200)
			tubeX[i] = width / 2 - topTube.width / 2 + width + i * distanceBetweenTubes
			topTubeRectangles[i] = Rectangle()
			bottomTubeRectangles[i] = Rectangle()
		}
	}

	/* This method is being called once every frame */
	override fun render() {
		batch.begin()
		batch.draw(background, 0f, 0f, width, height)

		when (gameState) {
			0 -> if (Gdx.input.justTouched()) gameState = 1
			1 -> {
				if (tubeX[scoringTube] < width / 2) {
					score++
					Gdx.app.log("Score", score.toString())
					scoringTube = if (scoringTube < numberOfTubes - 1) + 1 else 0
				}

				if (Gdx.input.justTouched()) velocity = -20f

				for (i in 0 until numberOfTubes) {
					if (tubeX[i] < -topTube.width) {
						tubeX[i] += numberOfTubes * distanceBetweenTubes
						tubeOffset[i] = (randomGenerator.nextFloat() - 0.5f) * (height - gap - 200)
					} else {
						tubeX[i] -= tubeVelocity
					}

					val topY = height / 2 + gap / 2 + tubeOffset[i]
					val bottomY = height / 2 - gap / 2 - bottomTube.height + tubeOffset[i]

					batch.draw(topTube, tubeX[i], topY)
					batch.draw(bottomTube, tubeX[i], bottomY)

					topTubeRectangles[i] = Rectangle(tubeX[i], topY, topTube.width.toFloat(), topTube.height.toFloat())
					bottomTubeRectangles[i] = Rectangle(tubeX[i], bottomY, bottomTube.width.toFloat(), bottomTube.height.toFloat())
				}

				if (birdY > 0) {
					velocity += gravity
					birdY -= velocity
				} else {
					gameState = 2
				}
			}
			2 -> {
				batch.draw(gameover, width / 2 - gameover.width / 2, height / 2 - gameover.height / 2)
				if (Gdx.input.justTouched()) {
					gameState = 1
					startGame()
					score = 0
					scoringTube = 0
					velocity = 0f
				}
			}
		}

		flapState = if (flapState == 0) 1 else 0
		batch.draw(birds[flapState], 0f + width / 2 - birds[flapState]!!.width / 2, birdY)
		font.draw(batch, score.toString(), 100f, 200f)
		birdCircle.set(width / 2, 0f + birdY + birds[flapState]!!.height / 2, 0f + birds[flapState]!!.width / 2)

		for (i in 0 until numberOfTubes)
			if (Intersector.overlaps(birdCircle, topTubeRectangles[i]) || Intersector.overlaps(birdCircle, bottomTubeRectangles[i]))
				gameState = 2
		batch.end()
	}

	/* This method is being called whenever the application is destroyed */
	override fun dispose() {
		batch.dispose()
	}
}