package com.tyron.code.tasks.git

import com.tyron.builder.project.Project
import org.eclipse.jgit.api.Git
import com.tyron.code.tasks.git.ErrorOutput
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.tyron.code.R
import com.tyron.code.databinding.BaseTextinputLayoutBinding
import com.tyron.code.databinding.LayoutDialogProgressBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.blankj.utilcode.util.ThreadUtils
import com.tyron.code.util.executeAsyncProvideError
import android.widget.Toast
import com.tyron.code.ui.ssh.callback.SshTransportConfigCallback
import com.tyron.code.tasks.git.GitProgressMonitor
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.merge.MergeResult

object GitMergeTask {
 
    val sshTransportConfigCallback =  SshTransportConfigCallback()

    fun merge(project:Project, context:Context) {
    
       val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.base_textinput_layout, null) 
       
       val binding = BaseTextinputLayoutBinding.inflate(inflater,null,false)
       binding.textinputLayout.setHint(R.string.set_branch)
       val builder = MaterialAlertDialogBuilder(context)
       builder.setTitle(R.string.title_branch_to_merge)
       builder.setView(binding.root)
     
       builder.setPositiveButton(R.string.title_merge) { dialog, _ ->
     
       val future =
       executeAsyncProvideError( {
       
       
       val branch = binding.textinputLayout.editText?.text?.toString()      
      
       if (branch.isNullOrBlank()) {
        ThreadUtils.runOnUiThread { Toast.makeText(context,context.getString(R.string.empty_branch),Toast.LENGTH_SHORT).show() }           
       }  else {
      
       val objectId:ObjectId? = Git.open(project.getRootFile()).getRepository().resolve("origin/" + branch)
       Git.open(project.getRootFile()).merge().include(objectId).setFastForward(MergeCommand.FastForwardMode.NO_FF).call()

       }       
           
       return@executeAsyncProvideError
           }, { _, _ -> } ) 
                  
       future.whenComplete { result, error ->
       ThreadUtils.runOnUiThread {
      
       if (result == null || error != null) {  ErrorOutput.ShowError(error, context)  }   
       else { Toast.makeText(context, context.getString(R.string.merge_completed),Toast.LENGTH_SHORT).show() }
       } }
    
       }
    
       builder.setNegativeButton(android.R.string.cancel, null)
       builder.show()
      
}
}
