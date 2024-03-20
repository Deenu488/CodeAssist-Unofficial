package $packagename

import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle

public class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         setContentView(R.layout.activity_main)
    }
}