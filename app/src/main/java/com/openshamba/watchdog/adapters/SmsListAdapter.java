package com.openshamba.watchdog.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.openshamba.watchdog.R;
import com.openshamba.watchdog.entities.Call;
import com.openshamba.watchdog.entities.Sms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Maina on 3/18/2018.
 */

public class SmsListAdapter extends RecyclerView.Adapter<SmsListAdapter.ViewHolder> {

    private SparseBooleanArray selectedItems;

    private List<Sms> original_items = new ArrayList<>();
    private List<Sms> filtered_items = new ArrayList<>();
    private SmsListAdapter.ItemFilter mFilter = new SmsListAdapter.ItemFilter();

    private Context ctx;
    private boolean clicked = false;

    public SmsListAdapter(Context ctx,List<Sms> items) {
        this.ctx = ctx;
        filtered_items = items;
        original_items = items;
        selectedItems = new SparseBooleanArray();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_sms, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new SmsListAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Sms s = filtered_items.get(position);
        holder.title.setText((s.getContact()));
        if(s.getType().equals("PERSONAL")){
            holder.content.setText("PERSONAL TEXT");
        }else{
            holder.content.setText(s.getCharge_code());
        }
        holder.time.setText(s.getTime());

        setAnimation(holder.itemView, position);

        holder.lyt_parent.setActivated(selectedItems.get(position, false));

        clicked = false;
    }

    @Override
    public int getItemCount() {
        return filtered_items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView title;
        public TextView content;
        public TextView time;
        public ImageView image;
        public LinearLayout lyt_parent;

        public ViewHolder(View v) {
            super(v);
            title = (TextView) v.findViewById(R.id.title2);
            content = (TextView) v.findViewById(R.id.content2);
            time = (TextView) v.findViewById(R.id.time2);
            image = (ImageView) v.findViewById(R.id.image2);
            lyt_parent = (LinearLayout) v.findViewById(R.id.lyt_parent2);
        }
    }

    private class ItemFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            String query = constraint.toString().toLowerCase();

            FilterResults results = new FilterResults();
            final List<Sms> list = original_items;
            final List<Sms> result_list = new ArrayList<>(list.size());

            for (int i = 0; i < list.size(); i++) {
                //TODO - add read contact name
                String str_title = list.get(i).getPhone();//getFriend().getName();
                if (str_title.toLowerCase().contains(query)) {
                    result_list.add(list.get(i));
                }
            }

            results.values = result_list;
            results.count = result_list.size();

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filtered_items = (List<Sms>) results.values;
            notifyDataSetChanged();
        }

    }

    /**
     * Here is the key method to apply the animation
     */
    private int lastPosition = -1;
    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(ctx, R.anim.slide_in_bottom);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    public Filter getFilter() {
        return mFilter;
    }

    public void addItems(List<Sms> sms) {
        this.original_items = sms;
        this.filtered_items = sms;
        notifyDataSetChanged();
    }
}
