package $packagename

import androidx.appcompat.app.AppCompatActivity
import $packagename.databinding.ActivityMainBinding
import android.os.Bundle

public class MainActivity : AppCompatActivity() {

	private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         binding = ActivityMainBinding.inflate(layoutInflater)
         val view = binding.root
         setContentView(view)
    }
}
