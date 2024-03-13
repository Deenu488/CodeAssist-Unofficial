package com.tyron.code.ui.project

import android.widget.Toast
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.blankj.utilcode.util.ThreadUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.transition.platform.MaterialSharedAxis
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.progress.ProgressMonitor
import org.apache.commons.io.FileUtils
import com.tyron.code.R
import com.tyron.code.databinding.FragmentProgressSheetDialogBinding
import com.tyron.code.util.TaskExecutor.executeAsyncProvideError
import java.io.File
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.io.IOException
import java.io.InputStreamReader
import android.os.Build
import android.os.Environment

class ImportProjectProgressFragment : BottomSheetDialogFragment() {
	
    companion object {
        fun newInstance(zipProjectUri: Uri): ImportProjectProgressFragment {
            val bundle = Bundle()
            bundle.putParcelable("zip_project_uri", zipProjectUri)
            val fragment = ImportProjectProgressFragment()
            fragment.arguments = bundle
            return fragment
        }

        @JvmField val TAG: String = ImportProjectProgressFragment::class.java.simpleName
    }
	
    private lateinit var binding: FragmentProgressSheetDialogBinding

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var future: CompletableFuture<ProgressMonitor.Result?>? = null
    private var progressMonitor: ProgressMonitor? = null
    private var zipProjectPath: File? = null
	 
    interface OnButtonClickedListener {
        fun onButtonClicked()
    }

    private var mOnButtonClickedListener: OnButtonClickedListener? = null

    fun setOnButtonClickedListener(listener: OnButtonClickedListener) {
        mOnButtonClickedListener = listener
    }
	
    interface OnSuccessListener {
        fun onSuccess()
    }

    private var mOnSuccessListener: OnSuccessListener? = null

    fun setOnSuccessListener(listener: OnSuccessListener) {
        mOnSuccessListener = listener
    }
	
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }
	
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProgressSheetDialogBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }
	
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val parent = view.parent as View
        val params = parent.layoutParams as CoordinatorLayout.LayoutParams
        bottomSheetBehavior = params.behavior as BottomSheetBehavior<View>?
        setImportProgress()
    }
	
    private fun setImportProgress() {
        bottomSheetBehavior?.isHideable = false
        bottomSheetBehavior?.isDraggable = true
        setCancelable(false)
        binding.textButton.isEnabled = false
        binding.textButton.text = getString(R.string.open_project)

        binding.textTitle.text = getString(R.string.importing_project)
        binding.textDescription.text = getString(R.string.preparing_for_import)
        binding.imageView.visibility = View.GONE
        binding.progressIndicator.visibility = View.VISIBLE

        val zipProjectUri: Uri? = requireArguments().getParcelable("zip_project_uri")
        val file = File(zipProjectUri?.path.toString())
        val split = file.path.split(":")
		
        val projectName = File(split[1].replace(".zip", "")).name
        
        val path: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireContext().getExternalFilesDir("/Projects")!!.absolutePath
        } else {
            Environment.getExternalStorageDirectory().toString() + "/CodeAssistProjects"                                    
        }

        val projectDir = File(path)
            if (!projectDir.exists()) {
            projectDir.mkdirs()
        }
                  
        val inputStream: InputStream? = zipProjectUri?.let {
            requireContext().contentResolver.openInputStream(it)
        }

        future = executeAsyncProvideError({
            inputStream?.let {
                zipProjectPath = inputStreamToFile(it)
            }

            val zipFile = ZipFile(zipProjectPath)
            zipFile.isRunInThread = true
            zipFile.extractAll(projectDir.absolutePath)

            progressMonitor = zipFile.progressMonitor
		
            while (progressMonitor?.state != ProgressMonitor.State.READY) {
                ThreadUtils.runOnUiThread {
                    binding.progressIndicator.isIndeterminate = false
                    binding.progressIndicator.progress = progressMonitor!!.percentDone.toInt()
                    binding.textDescription.text = "${progressMonitor?.percentDone}%\n${getString(R.string.imported)}"
                }

                Thread.sleep(100)
            }

            return@executeAsyncProvideError progressMonitor?.result
        }, { _, _ -> })
			
        future?.whenComplete { result, error ->
            FileUtils.deleteQuietly(zipProjectPath)
            ThreadUtils.runOnUiThread {
                if (result != null && error == null) {
                    when (result) {
                        ProgressMonitor.Result.SUCCESS -> {
                            mOnSuccessListener?.onSuccess()
                            setCancelable(true)
                            bottomSheetBehavior?.isHideable = true
                            binding.progressIndicator.progress = progressMonitor!!.percentDone.toInt()
                            binding.textDescription.text = "${progressMonitor?.percentDone}%"
                            binding.progressIndicator.visibility = View.GONE
                            binding.imageView.visibility = View.VISIBLE
                            binding.imageView.setImageResource(R.drawable.ic_done)
                            binding.textTitle.text = getString(R.string.import_success)
                            binding.textDescription.text = getString(R.string.project_imported_successfully)
                            binding.textButton.isEnabled = true
                            binding.textButton.setOnClickListener {
                                mOnButtonClickedListener?.onButtonClicked()
                            }
                        }
                        ProgressMonitor.Result.ERROR -> {
                            setCancelable(true)
                            bottomSheetBehavior?.isHideable = true
                            binding.progressIndicator.visibility = View.GONE
                            binding.imageView.visibility = View.VISIBLE
						
                            binding.imageView.setImageResource(R.drawable.ic_error)
                            binding.textTitle.text = getString(R.string.import_failed)
                            binding.textDescription.text = progressMonitor?.exception?.message
                            binding.textButton.isEnabled = true
                            binding.textButton.text = getString(R.string.okay)
                            binding.textButton.setOnClickListener {
                                dismiss()
                            }
                        }
                        ProgressMonitor.Result.CANCELLED -> {
                            dismiss()
                            showToast(getString(R.string.project_import_cancelled))
                        }
                        ProgressMonitor.Result.WORK_IN_PROGRESS -> {
                        }
                    }
                } else {
                    setCancelable(true)
                    bottomSheetBehavior?.isHideable = true
                    binding.progressIndicator.visibility = View.GONE
                    binding.imageView.visibility = View.VISIBLE
						
                    binding.imageView.setImageResource(R.drawable.ic_error)
                    binding.textTitle.text = getString(R.string.import_failed)
                    binding.textDescription.text = error.localizedMessage
                    binding.textButton.isEnabled = true
                    binding.textButton.text = getString(R.string.okay)
                    binding.textButton.setOnClickListener {
                        dismiss()
                    }
                }
            }
        }
    }
    
    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(requireContext(), message, duration).show()
    }
	
    fun inputStreamToFile(inputStream: InputStream): File {
        val outputFile = tempFile()
        FileUtils.copyInputStreamToFile(inputStream, outputFile)
        return outputFile
    }

    fun tempFile(): File {
        val tempDir = requireContext().cacheDir
        val outputFile = File(tempDir, "temp_file.tmp")
        return outputFile
    }
}