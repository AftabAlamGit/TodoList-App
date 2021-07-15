package com.example.todolist;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.todolist.database.AppDatabase;
import com.example.todolist.database.TaskEntry;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import static androidx.recyclerview.widget.DividerItemDecoration.VERTICAL;

public class MainActivity extends AppCompatActivity implements TaskAdapter.ItemClickListener {

    // Constant for logging
    private static final String TAG = MainActivity.class.getSimpleName();
    // Member variables for the adapter and RecyclerView
    private RecyclerView mRecyclerView;
    private TaskAdapter mAdapter;

    // AppDatabase member variable for the Database
    private AppDatabase mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the RecyclerView to its corresponding view
        mRecyclerView = findViewById(R.id.recyclerViewTasks);

        // Set the layout for the RecyclerView to be a linear layout, which measures and
        // positions items within a RecyclerView into a linear list
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the adapter and attach it to the RecyclerView
        mAdapter = new TaskAdapter(this, this);
        mRecyclerView.setAdapter(mAdapter);

        DividerItemDecoration decoration = new DividerItemDecoration(getApplicationContext(), VERTICAL);
        mRecyclerView.addItemDecoration(decoration);

        /*
         Add a touch helper to the RecyclerView to recognize when a user swipes to delete an item.
         An ItemTouchHelper enables touch behavior (like swipe and move) on each ViewHolder,
         and uses callbacks to signal when a user is performing these actions.
         */
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            // Called when a user swipes left or right on a ViewHolder
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {

                // Here is where you'll implement swipe to delete
                // COMPLETED (1) Get the diskIO Executor from the instance of AppExecutors and
                // call the diskIO execute method with a new Runnable and implement its run method
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        // get the position from the viewHolder parameter
                        int position = viewHolder.getBindingAdapterPosition();
                        List<TaskEntry> tasks = mAdapter.getTasks();
                        // Call deleteTask in the taskDao with the task at that position
                        mDb.taskDao().deleteTask(tasks.get(position));
                    }
                });
            }
        }).attachToRecyclerView(mRecyclerView);

        /*
         Set the Floating Action Button (FAB) to its corresponding View.
         Attach an OnClickListener to it, so that when it's clicked, a new intent will be created
         to launch the AddTaskActivity.
         */
        FloatingActionButton fabButton = findViewById(R.id.fab);

        fabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create a new intent to start an AddTaskActivity
                Intent addTaskIntent = new Intent(MainActivity.this, AddTaskActivity.class);
                startActivity(addTaskIntent);
            }
        });

        // Initialize member variable for the data base
        mDb = AppDatabase.getInstance(getApplicationContext());
        retrieveTasks();
    }

    private void retrieveTasks() {
        Log.d(TAG, "Actively retrieving the tasks from the DataBase");
        // Extract all this logic outside the Executor and remove the Executor
        // Fix compile issue by wrapping the return type with LiveData
        LiveData<List<TaskEntry>> tasks = mDb.taskDao().loadAllTasks();
        // Observe tasks and move the logic from runOnUiThread to onChanged
        tasks.observe(this, new Observer<List<TaskEntry>>() {
            @Override
            public void onChanged(@Nullable List<TaskEntry> taskEntries) {
                Log.d(TAG, "Receiving database update from LiveData");
                mAdapter.setTasks(taskEntries);
            }
        });
    }

    @Override
    public void onItemClickListener(int itemId) {

        // Launch AddTaskActivity with itemId as extra for the key AddTaskActivity.EXTRA_TASK_ID
        Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
        intent.putExtra(AddTaskActivity.EXTRA_TASK_ID, itemId);
        startActivity(intent);
    }
}