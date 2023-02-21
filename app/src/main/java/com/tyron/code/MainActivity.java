package com.tyron.code;

import android.os.Bundle;
import android.view.KeyEvent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import com.tyron.code.ui.main.HomeFragment;

public class MainActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

    HomeFragment homeFragment = new HomeFragment();
    if (getSupportFragmentManager().findFragmentByTag(HomeFragment.TAG) == null) {
      getSupportFragmentManager()
          .beginTransaction()
          .replace(R.id.fragment_container, homeFragment, HomeFragment.TAG)
          .commit();
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
  }

  @Override
  public boolean onKeyShortcut(int keyCode, KeyEvent event) {
    return super.onKeyShortcut(keyCode, event);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    return super.onKeyUp(keyCode, event);
  }
}
