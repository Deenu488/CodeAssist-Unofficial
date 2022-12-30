package com.tyron.code.tasks.git

import android.content.Context
import com.tyron.builder.project.Project
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tyron.code.tasks.git.GitInitTask


object GitTask {
    
    fun showTasks(context:Context, project:Project) {
    
    val option = arrayOf(
            "Init",   
            "Status",
            "Fetch",
            "Add all to stage",
            "Commit",
            "Branch",
            "Remote",
            "Push",
            "Pull",
            "Merge",
            "Reset (HARD)",
            )
                    
       val builder = MaterialAlertDialogBuilder(context)  
       builder.setItems(option) { _, which -> 
       when (which) {
       
		 0 -> {  GitInitTask.init(project)  }
		 1 -> { }
		 2 -> { }
		 3 -> { }
		 4 -> { }
		 5 -> { }
		 6 -> { }
		 7 -> { }
		 8 -> { }
		 9 -> { }
		 10 -> { }
		 11 -> { }
		 
		 }
		 }
		 builder.show()
		 
    }
}
