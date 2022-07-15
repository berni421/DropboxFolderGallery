package com.elbourn.andriod.dropboxfoldergallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;


public class SelectPictureViewAdapter extends RecyclerView.Adapter<SelectPictureViewAdapter.ViewHolder> {

    private static String APP = BuildConfig.APPLICATION_ID;
    private static String TAG = "SelectPictureViewAdapter";
    int columns = 6;
    int lastPosition = 0;
    ArrayList<ArrayList<Integer>> positions = null;

    private LayoutInflater mInflater;
    private List<GraphicData> mData;
    private ItemClickListener mClickListener;
    private ItemLongClickListener mLongClickListener;
    private Context context = null;

    // data is passed into the constructor
    SelectPictureViewAdapter(Context context, List<GraphicData> data) {
        mInflater = LayoutInflater.from(context);
        mData = data;
        context = context;
        lastPosition = 0;
        columns = 6;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.activity_show_pictures_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int row) {
        Log.i(TAG, "start myOnBindViewHolder");
        Log.i(TAG, "row: " + row);
        int columnsInRow = positions.get(row).size();
        int column;
        for (column=0; column<columnsInRow; column++) {
            Log.i(TAG, "setting column: " + column);
            int position = positions.get(row).get(column);
            Bitmap thumbnail = mData.get(position).thumbnail;
            holder.imageViews.get(column).setImageBitmap(thumbnail);
            holder.imageViews.get(column).setTransitionName(String.valueOf(position));
            if (column == 0) {
                String thisFolder = mData.get(position).onlineFolder;
                holder.myFolderName.setText(thisFolder);
                if (position > 0) {
                    String lastFolder = mData.get(position - 1).onlineFolder;
                    if (lastFolder.equals(thisFolder)){
                        holder.myFolderName.setVisibility(View.GONE);
                    } else{
                        holder.myFolderName.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
        while (column < columns) {
            Log.i(TAG, "resetting column: " + column);
            holder.imageViews.get(column).setImageBitmap(null);
            holder.imageViews.get(column).setTransitionName(null);
            column++;
        }
        Log.i(TAG, "end myOnBindViewHolder");
    }

    void addRow(int position) {
        // new row
        positions.add(new ArrayList<>());
        int row = positions.size()-1;
        positions.get(row).add(position);
    }

    void addColumn(int position) {
        int row = positions.size()-1;
        int column = positions.get(row).size()-1;
        if (column < columns-1) {
            positions.get(row).add(position);
        } else {
            addRow(position);
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        Log.i(TAG, "start getItemCount");
        positions = new ArrayList<>();
        String last = mData.get(0).onlineFolder;
        addRow(0);
        int position = 1;
        int items = mData.size();
        Log.i(TAG, "items: " + items);
        while (position < items) {
            String current = mData.get(position).onlineFolder;
                if (current.equals(last)) {
                    addColumn(position);
                } else {
                    addRow(position);
                }
            position++;
            last = current;
        }
        int rows = positions.size();
        Log.i(TAG, "total rows: " + rows);
        Log.i(TAG, "end getItemCount");
        return rows;
    }

    void dumpPositions() {
        int rows = positions.size();
        for (int r = 0; r < rows; r++) {
            Log.i(TAG, "row: " + r);
            Log.i(TAG, "columns: " + positions.get(r));
        }
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,View.OnLongClickListener {
        TextView myFolderName;
        ArrayList<ImageView> imageViews = new ArrayList<>();
        ViewHolder(View itemView) {
            super(itemView);
            myFolderName = itemView.findViewById(R.id.folderName);
            imageViews.add(itemView.findViewById(R.id.myImage00));
            imageViews.get(0).setOnClickListener(this);
            imageViews.get(0).setOnLongClickListener(this);
            imageViews.add(itemView.findViewById(R.id.myImage01));
            imageViews.get(1).setOnClickListener(this);
            imageViews.get(1).setOnLongClickListener(this);
            imageViews.add(itemView.findViewById(R.id.myImage02));
            imageViews.get(2).setOnClickListener(this);
            imageViews.get(2).setOnLongClickListener(this);
            imageViews.add(itemView.findViewById(R.id.myImage03));
            imageViews.get(3).setOnClickListener(this);
            imageViews.get(3).setOnLongClickListener(this);
            imageViews.add(itemView.findViewById(R.id.myImage04));
            imageViews.get(4).setOnClickListener(this);
            imageViews.get(4).setOnLongClickListener(this);
            imageViews.add(itemView.findViewById(R.id.myImage05));
            imageViews.get(5).setOnClickListener(this);
            imageViews.get(5).setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null && view.getTransitionName() != null) {
                mClickListener.onItemClick(view, Integer.parseInt(view.getTransitionName()));
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if (mLongClickListener != null && view.getTransitionName() != null) {
                mLongClickListener.onLongItemClick(view, Integer.parseInt(view.getTransitionName()));
            }
            return true;
        }

    }

    // convenience method for getting data at click position
    GraphicData getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int p);
    }

    // allows clicks events to be caught
    void setLongClickListener(ItemLongClickListener itemLongClickListener) {
        mLongClickListener = itemLongClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemLongClickListener {
        void onLongItemClick(View view, int p);
    }
}
