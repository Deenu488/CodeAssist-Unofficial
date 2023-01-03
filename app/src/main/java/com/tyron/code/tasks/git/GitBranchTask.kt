package com.tyron.code.tasks.git

import com.tyron.builder.project.Project
import org.eclipse.jgit.api.Git
import com.tyron.code.tasks.git.ErrorOutput
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.tyron.code.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.blankj.utilcode.util.ThreadUtils
import com.tyron.code.util.executeAsyncProvideError
import android.widget.Toast
import com.tyron.code.databinding.BaseTextinputLayoutBinding

object GitBranchTask {
    
       fun branch(project:Project, context:Context) {
    
       val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.base_textinput_layout, null)
       
       val binding = BaseTextinputLayoutBinding.inflate(inflater,null,false)
       binding.textinputLayout.setHint(R.string.branch_name)
       val builder = MaterialAlertDialogBuilder(context)
       builder.setTitle(R.string.title_create_remove_branch)
       builder.setView(binding.root)
       builder.setPositiveButton(R.string.wizard_create) { _, _ ->
         
       val future =
       executeAsyncProvideError( {
       val branch = binding.textinputLayout.editText?.text?.toString()
 
       if (branch.isNullOrBlank()) {
       ThreadUtils.runOnUiThread { Toast.makeText(context, context.getString(R.string.empty_branch), Toast.LENGTH_SHORT).show() }           
       } else {  
    
       Git.open(project.getRootFile()).checkout().setName(branch).setCreateBranch(true) .call()
        
       }
       
       return@executeAsyncProvideError
       }, { _, _ -> } )  
       
       future.whenComplete { result, error ->
       ThreadUtils.runOnUiThread {
      
       if (result == null || error != null) { ErrorOutput.ShowError(error, context) }  
       else { Toast.makeText(context,context.getString(R.string.branch_created_successfully),Toast.LENGTH_SHORT).show() }
       } } 
       
       }
       
       builder.setNegativeButton(android.R.string.cancel, null)
       builder.setNeutralButton(R.string.remove) { _, _ -> 
       
       val future =
       executeAsyncProvideError( {
       val branch = binding.textinputLayout.editText?.text?.toString()
 
       if (branch.isNullOrBlank()) {
       ThreadUtils.runOnUiThread { Toast.makeText(context, context.getString(R.string.empty_branch), Toast.LENGTH_SHORT).show() }           
       } else {  
    
        Git.open(project.getRootFile()).branchDelete().setBranchNames(branch).setForce(true).call()
    
       }
       
       return@executeAsyncProvideError
       }, { _, _ -> } )  
       
       future.whenComplete { result, error ->
       ThreadUtils.runOnUiThread {
      
       if (result == null || error != null) { ErrorOutput.ShowError(error, context) }  
       else { Toast.makeText(context,context.getString(R.string.branch_removed_successfully),Toast.LENGTH_SHORT).show() }
       } } 
       
        }
	   builder.show()	 
    
}
}
