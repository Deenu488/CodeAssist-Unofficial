package com.tyron.code.ui.git

import android.os.Build
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.tyron.code.R
import com.tyron.code.databinding.BaseTextinputLayoutBinding
import com.tyron.code.databinding.LayoutDialogProgressBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Environment
import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import com.tyron.code.util.executeAsyncProvideError
import android.widget.TextView
import com.blankj.utilcode.util.ThreadUtils
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.eclipse.jgit.api.CloneCommand
import com.tyron.builder.project.Project
import org.eclipse.jgit.api.Status
import android.widget.Toast
object GitClone {
       
       fun cloneGitRepo(context:Context) {
   	   val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.base_textinput_layout, null)
	   
	   val binding = BaseTextinputLayoutBinding.inflate(inflater,null,false)
	   binding.textinputLayout.setHint(R.string.git_clone_repo_url)
	   val builder = MaterialAlertDialogBuilder(context)
	   builder.setTitle(R.string.git_clone_repo)
	   builder.setView(binding.root)
	   builder.setCancelable(true)
       builder.setPositiveButton(R.string.git_clone) { dialog, _ ->
       dialog.dismiss()
       val url = binding.textinputLayout.editText?.text?.toString()
       cloneRepo(url, context)
       }
       builder.setNegativeButton(android.R.string.cancel, null)
   
       builder.show()
       }			
	   private fun cloneRepo(repo: String?, context:Context) {
       if (repo.isNullOrBlank()) {
       return
       }

       var url = repo.trim()
       if (!url.endsWith(".git")) {
       url += ".git"
       }
	   
	   val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.layout_dialog_progress, null)
	   
	   val binding = LayoutDialogProgressBinding.inflate(inflater,null,false)
	   val view = binding.root
	   
	   binding.message.visibility = View.VISIBLE
	  
	   val builder = MaterialAlertDialogBuilder(context)
       builder.setTitle(R.string.git_clone_in_progress)
       builder.setMessage(url)
       builder.setView(view)
       builder.setCancelable(false)
	   
	   val repoName = url.substringAfterLast('/').substringBeforeLast(".git")
		   
	   val targetDir:File
	   
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
	   targetDir = File(context.getExternalFilesDir("/Projects"), repoName)
       } else {  
	   targetDir = File(Environment.getExternalStorageDirectory().absolutePath + "/CodeAssistProjects"  , repoName)
       }
	   
	   val progress = GitCloneProgressMonitor(binding.progress, binding.message)
       var git: Git? = null
	    
	   val future =
	   executeAsyncProvideError(
	   {
	   val cloneCommand: CloneCommand =  Git.cloneRepository()
	   cloneCommand.setURI(url)
 	   cloneCommand.setDirectory(targetDir)
	   
	   if(url.startsWith("git@github.com") && url.startsWith("ssh://git@github.com")) {
      // cloneCommand.setTransportConfigCallback(sshTransportConfigCallback)
       }
		
       cloneCommand.setProgressMonitor(progress)
  	   cloneCommand.call()
	   .also { git = it }
	   return@executeAsyncProvideError
	   },
	   { _, _ -> }
	   )
	   
	   builder.setPositiveButton(android.R.string.cancel) { iface, _ ->
       iface.dismiss()
       progress.cancel()
       git?.close()
       future.cancel(true)
       }
       
 	  val dialog = builder.show() 
	   
	   future.whenComplete { result, error ->
       ThreadUtils.runOnUiThread {
       dialog?.dismiss()
     
       if (result == null || error != null) {
       if (!future.isCancelled) {
	   showCloneError(error, context)
       }
       } else {
       
	   val builder = MaterialAlertDialogBuilder(context)
       builder.setTitle(R.string.success)
	   builder.setMessage(url+" " + context.getString(R.string.cloned_successfully))
       builder.setPositiveButton(android.R.string.ok, null)
       builder.show()
	   }
       }
       }
	   }
	   
	   private fun showCloneError(error: Throwable, context:Context) {
	   val builder = MaterialAlertDialogBuilder(context)
       builder.setTitle(R.string.git_clone_failed)
       builder.setMessage(error.localizedMessage)
       builder.setPositiveButton(android.R.string.ok, null)
       builder.show()
	   }
	   
	   class GitCloneProgressMonitor(val progress: LinearProgressIndicator, val message: TextView) :
       ProgressMonitor {

       private var cancelled = false

       fun cancel() {
       cancelled = true
       }

       override fun start(totalTasks: Int) {
       ThreadUtils.runOnUiThread { progress.max = totalTasks }
       }
  
       override fun beginTask(title: String?, totalWork: Int) {
       ThreadUtils.runOnUiThread { message.text = title }
       }

       override fun update(completed: Int) {
       ThreadUtils.runOnUiThread { progress.progress = completed }
       }

       override fun endTask() {}

       override fun isCancelled(): Boolean {
       return cancelled || Thread.currentThread().isInterrupted
       }
       }
       fun gitActions(context:Context, project:Project) {
       val option = arrayOf(
            "Init",			
            "Status",
			"Fetch",
		    "Commit",
            "Push",
            "Pull"
            )
					
	   val builder = MaterialAlertDialogBuilder(context)
       
	   builder.setItems(option) { _, which ->
	 
	   when (which) {
		 0 -> { val future =
	   executeAsyncProvideError(
	   {
		var git: Git? = null
		git = Git.init().setDirectory(project.getRootFile()).call()
	   
	   .also { git = it }
	   
	   return@executeAsyncProvideError
	   },
	   { _, _ -> }   
	   )  
	   
	   future.whenComplete { result, error ->
	   ThreadUtils.runOnUiThread {
	  
       if (result == null || error != null) {
	   showError(error, context)
       } else {
       Toast.makeText(context,"Git repositoty initialize success",Toast.LENGTH_SHORT).show()  

        }   
      }  }
       }     
	       1 -> { 
	       
	val future =
	   executeAsyncProvideError(
	   {
		 var git: Git? = null
		
		
		var status: Status =    Git.open(project.getRootFile()).status().call()
	  	 
		 ThreadUtils.runOnUiThread {
	 	 val builder = MaterialAlertDialogBuilder(context)      
    	 var st : String = "Added: " + status.getAdded() + "\n" + "Changed: " + status.getChanged()+ "\n" + "Conflicting: " + status.getConflicting()+ "\n" +	 "ConflictingStageState: " + status.getConflictingStageState()+ "\n" + "IgnoredNotInIndex: " + status.getIgnoredNotInIndex()+ "\n" +	 "Missing: " + status.getMissing()+ "\n" +	"Modified: " + status.getModified()+ "\n" +	"Removed: " + status.getRemoved()+ "\n" +	 "Untracked: " + status.getUntracked()+ "\n" + "UntrackedFolders: " + status.getUntrackedFolders()
    	 builder.setMessage(st)	   
         builder.show()
	   }	 	   
	   return@executeAsyncProvideError
	   
	   },
	   { _, _ -> }   
	   )  
	   future.whenComplete { result, error ->
	   ThreadUtils.runOnUiThread {
	  
       if (result == null || error != null) {
	   showError(error, context)
       }    } }
	       
	       }
	     
	       }   }
	   builder.show()
	   }
	   private fun showError(error: Throwable, context:Context) {
	   val builder = MaterialAlertDialogBuilder(context)
       builder.setMessage(error.localizedMessage)
       builder.show()
	   }
	   }
	   
	   
