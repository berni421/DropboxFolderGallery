package com.elbourn.andriod.dropboxfoldergallery;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;


public class SelectFolderViewAdapter extends RecyclerView.Adapter<SelectFolderViewAdapter.ViewHolder> {

    private static String APP = BuildConfig.APPLICATION_ID;
    private static String TAG = "SelectFolderViewAdapter";
    public static int columns = 2;

    private List<String> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Context context = null;

    // data is passed into the constructor
    SelectFolderViewAdapter(Context context, List<String> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.context = context;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.activity_show_folders_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int row) {
        int position = row * columns;
        for (int i = 0; i < columns; i++) {
            String folder = mData.get(row);
                switch (i) {
                    case 0:
                        if (folder == context.getString(R.string.nosubfoldershere)) {
                            holder.playButton.setVisibility(View.INVISIBLE);
                        } else {
                            holder.playButton.setVisibility(View.VISIBLE);
                        }
                        holder.myFolder.setText(folder);
                        holder.myFolder.setTransitionName(String.valueOf(position));
                        break;
                    case 1:
                        holder.playButton.setTransitionName(String.valueOf(position + 1));
                        break;
            }
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView myFolder;
        ImageButton playButton;

        ViewHolder(View itemView) {
            super(itemView);
            myFolder = itemView.findViewById(R.id.myFolder);
            myFolder.setOnClickListener(this);
            playButton = itemView.findViewById(R.id.play);
            playButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) {
                mClickListener.onItemClick(view, Integer.parseInt(view.getTransitionName()));
            }
        }
    }

    // convenience method for getting data at click position
    String getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int p);
    }
}
