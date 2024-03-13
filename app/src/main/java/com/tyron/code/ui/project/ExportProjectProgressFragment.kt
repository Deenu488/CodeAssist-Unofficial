package com.tyron.code.ui.project

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
import org.apache.commons.io.FileUtils
import com.tyron.code.R
import com.tyron.code.databinding.FragmentProgressSheetDialogBinding
import com.tyron.code.util.TaskExecutor.executeAsyncProvideError
import java.io.File

class ExportProjectProgressFragment : BottomSheetDialogFragment() {
	
    companion object {
        fun newInstance(currentProject: String, zipProjectUri: Uri): ExportProjectProgressFragment {
            val bundle = Bundle()
            bundle.putString("current_project", currentProject)
            bundle.putParcelable("zip_project_uri", zipProjectUri)
            val fragment = ExportProjectProgressFragment()
            fragment.arguments = bundle
            return fragment
        }

        @JvmField val TAG: String = ExportProjectProgressFragment::class.java.simpleName
    }
	
    private lateinit var binding: FragmentProgressSheetDialogBinding

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
	
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
        setExportProgress()
    }
	
    private fun setExportProgress() {
        bottomSheetBehavior?.isHideable = false
        bottomSheetBehavior?.isDraggable = true
        setCancelable(false)
        binding.textButton.isEnabled = false
        binding.textButton.text = getString(R.string.close)
	
        binding.textTitle.text = getString(R.string.exporting_project)
        binding.textDescription.text = getString(R.string.exporting)
        binding.imageView.visibility = View.GONE
        binding.progressIndicator.visibility = View.VISIBLE

        val future = executeAsyncProvideError({
            val zipProjectUri: Uri? = requireArguments().getParcelable("zip_project_uri")
            val currentProject = requireArguments().getString("current_project")
            val project = File(currentProject.toString())
			
            val tempFile = tempFile()

            val zipFile = ZipFile(tempFile)
            zipFile.addFolder(project)
            zipFile.isRunInThread = true
			
            val byteArray: ByteArray = FileUtils.readFileToByteArray(tempFile)
  			
            zipProjectUri?.let {
                requireContext().contentResolver.openOutputStream(zipProjectUri).use { outputStream ->
                    outputStream?.write(byteArray)
                }
            }
			
            return@executeAsyncProvideError
        }, { _, _ -> })
			
        future.whenComplete { result, error ->
            FileUtils.deleteQuietly(tempFile())
            ThreadUtils.runOnUiThread {
                if (result != null || error == null) {
                    mOnSuccessListener?.onSuccess()
                    setCancelable(true)
                    bottomSheetBehavior?.isHideable = true
                    binding.progressIndicator.visibility = View.GONE
                    binding.imageView.visibility = View.VISIBLE
                    binding.imageView.setImageResource(R.drawable.ic_done)
                    binding.textTitle.text = getString(R.string.export_success)
                    binding.textDescription.text = getString(R.string.project_exported_successfully)
                    binding.textButton.isEnabled = true
                    binding.textButton.setOnClickListener {
                        dismiss()
                    }
                } else {
                    setCancelable(true)
                    bottomSheetBehavior?.isHideable = true
                    binding.progressIndicator.visibility = View.GONE
                    binding.imageView.visibility = View.VISIBLE
						
                    binding.imageView.setImageResource(R.drawable.ic_error)
                    binding.textTitle.text = getString(R.string.export_failed)
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

    fun tempFile(): File {
        val tempDir = requireContext().cacheDir
        val outputFile = File(tempDir, "temp_file.tmp")
        return outputFile
    }
}