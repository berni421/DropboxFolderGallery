package com.elbourn.andriod.dropboxfoldergallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.List;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;


public class SelectPictureViewAdapter extends RecyclerView.Adapter<SelectPictureViewAdapter.ViewHolder> {

    private static String APP = BuildConfig.APPLICATION_ID;
    private static String TAG = "SelectPictureViewAdapter";
    static int columns = 6;

    private List<GraphicData> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Context context = null;

    // data is passed into the constructor
    SelectPictureViewAdapter(Context context, List<GraphicData> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.context = context;
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
        int sizemData = mData.size();
        Log.i(TAG, "sizemData: " + sizemData);
        for (int column = 0; column < columns; column++) {
            int position = row * columns + column;
            Log.i(TAG, "position: " + position);
            Bitmap thumbnail = null;
            if (position < sizemData) {
                thumbnail = mData.get(position).thumbnail;
            }
            if (thumbnail == null) {
                Log.e(TAG, "thumbnail is null");
            }
            switch (column) {
                case 0:
                    holder.myImageView00.setImageBitmap(thumbnail);
                    holder.myImageView00.setTransitionName(String.valueOf(position));
                    break;
                case 1:
                    holder.myImageView01.setImageBitmap(thumbnail);
                    holder.myImageView01.setTransitionName(String.valueOf(position));
                    break;
                case 2:
                    holder.myImageView02.setImageBitmap(thumbnail);
                    holder.myImageView02.setTransitionName(String.valueOf(position));
                    break;
                case 3:
                    holder.myImageView03.setImageBitmap(thumbnail);
                    holder.myImageView03.setTransitionName(String.valueOf(position));
                    break;
                case 4:
                    holder.myImageView04.setImageBitmap(thumbnail);
                    holder.myImageView04.setTransitionName(String.valueOf(position));
                    break;
                case 5:
                    holder.myImageView05.setImageBitmap(thumbnail);
                    holder.myImageView05.setTransitionName(String.valueOf(position));
                    break;
            }
        }
        Log.i(TAG, "end myOnBindViewHolder");
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return 1 + (mData.size() - 1) / columns;
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView myImageView00;
        ImageView myImageView01;
        ImageView myImageView02;
        ImageView myImageView03;
        ImageView myImageView04;
        ImageView myImageView05;

        ViewHolder(View itemView) {
            super(itemView);
            myImageView00 = itemView.findViewById(R.id.myImage00);
            myImageView00.setOnClickListener(this);
            myImageView01 = itemView.findViewById(R.id.myImage01);
            myImageView01.setOnClickListener(this);
            myImageView02 = itemView.findViewById(R.id.myImage02);
            myImageView02.setOnClickListener(this);
            myImageView03 = itemView.findViewById(R.id.myImage03);
            myImageView03.setOnClickListener(this);
            myImageView04 = itemView.findViewById(R.id.myImage04);
            myImageView04.setOnClickListener(this);
            myImageView05 = itemView.findViewById(R.id.myImage05);
            myImageView05.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) {
                mClickListener.onItemClick(view, Integer.parseInt(view.getTransitionName()));

            }
        }
    }

    // convenience method for getting data at click position
    GraphicData getItem(int id) {
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
