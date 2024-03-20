package $packagename;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import java.util.Random;

public class GameSuperClass extends ApplicationAdapter {
  private BitmapFont font;
  private Circle birdCircle;
  private Random randomGenerator;
  private SpriteBatch batch; // Used to draw sprite and textures onto the screen
  private Texture topTube;
  private Texture bottomTube;
  private Texture background;
  private Texture gameover; // Texture, that will be drawn to a screen

  private float birdY;
  private float distanceBetweenTubes;
  private float maxTubeOffset;
  private float velocity;

  private int flapState;
  private int score;
  private int scoringTube;
  private int gameState;
  private int width;
  private int height;

  private final float gap = 400f;
  private final float tubeVelocity = 4f;
  private final int numberOfTubes = 4;
  private final int gravity = 2;

  private final float[] tubeX = new float[numberOfTubes];
  private final float[] tubeOffset = new float[numberOfTubes];

  private final Rectangle[] topTubeRectangles = new Rectangle[numberOfTubes];
  private final Rectangle[] bottomTubeRectangles = new Rectangle[numberOfTubes];
  private final Texture[] birds = new Texture[2];

  /* This method is being called when application is created */
  @Override
  public void create() {
    batch = new SpriteBatch();
    background = new Texture("bg.png");
    gameover = new Texture("gameover.png");
    birdCircle = new Circle();
    font = new BitmapFont();
    font.setColor(Color.WHITE);
    font.getData().setScale(10);

    width = Gdx.graphics.getWidth();
    height = Gdx.graphics.getHeight();

    birds[0] = new Texture("bird.png");
    birds[1] = new Texture("bird2.png");

    topTube = new Texture("toptube.png");
    bottomTube = new Texture("bottomtube.png");
    maxTubeOffset = height / 2 - gap / 2 - 100;
    randomGenerator = new Random();
    distanceBetweenTubes = width * 3 / 4;

    startGame();
  }

  private void startGame() {
    birdY = height / 2 - birds[0].getHeight() / 2;

    for (int i = 0; i < numberOfTubes; i++) {
      tubeOffset[i] = (randomGenerator.nextFloat() - 0.5f) * (height - gap - 200);
      tubeX[i] = width / 2 - topTube.getWidth() / 2 + width + i * distanceBetweenTubes;
      topTubeRectangles[i] = new Rectangle();
      bottomTubeRectangles[i] = new Rectangle();
    }
  }

  /* This method is being called once every frame */
  @Override
  public void render() {
    batch.begin();
    batch.draw(background, 0, 0, width, height);

    switch (gameState) {
      case 0:
        if (Gdx.input.justTouched()) gameState = 1;
        break;
      case 1:
        if (tubeX[scoringTube] < width / 2) {
          score++;
          Gdx.app.log("Score", String.valueOf(score));
          scoringTube = (scoringTube < numberOfTubes - 1) ? +1 : 0;
        }

        if (Gdx.input.justTouched()) velocity = -20;

        for (int i = 0; i < numberOfTubes; i++) {
          if (tubeX[i] < -topTube.getWidth()) {
            tubeX[i] += numberOfTubes * distanceBetweenTubes;
            tubeOffset[i] = (randomGenerator.nextFloat() - 0.5f) * (height - gap - 200);
          } else {
            tubeX[i] -= tubeVelocity;
          }

          final float topY = height / 2 + gap / 2 + tubeOffset[i];
          final float bottomY = height / 2 - gap / 2 - bottomTube.getHeight() + tubeOffset[i];

          batch.draw(topTube, tubeX[i], topY);
          batch.draw(bottomTube, tubeX[i], bottomY);

          topTubeRectangles[i] =
              new Rectangle(tubeX[i], topY, topTube.getWidth(), topTube.getHeight());
          bottomTubeRectangles[i] =
              new Rectangle(tubeX[i], bottomY, bottomTube.getWidth(), bottomTube.getHeight());
        }

        if (birdY > 0) {
          velocity += gravity;
          birdY -= velocity;
        } else {
          gameState = 2;
        }
        break;
      case 2:
        batch.draw(
            gameover, width / 2 - gameover.getWidth() / 2, height / 2 - gameover.getHeight() / 2);
        if (Gdx.input.justTouched()) {
          gameState = 1;
          startGame();
          score = 0;
          scoringTube = 0;
          velocity = 0;
        }
        break;
    }

    flapState = (flapState == 0) ? 1 : 0;
    batch.draw(birds[flapState], width / 2 - birds[flapState].getWidth() / 2, birdY);
    font.draw(batch, String.valueOf(score), 100, 200);
    birdCircle.set(
        width / 2, birdY + birds[flapState].getHeight() / 2, birds[flapState].getWidth() / 2);

    for (int i = 0; i < numberOfTubes; i++)
      if (Intersector.overlaps(birdCircle, topTubeRectangles[i])
          || Intersector.overlaps(birdCircle, bottomTubeRectangles[i])) gameState = 2;
    batch.end();
  }

  /* This method is being called whenever the application is destroyed */
  @Override
  public void dispose() {
    batch.dispose();
  }
}
