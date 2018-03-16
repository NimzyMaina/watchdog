package com.openshamba.watchdog.adapters;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maina on 3/16/2018.
 */

public class CallsListAdapter extends RecyclerView.Adapter<CallsListAdapter.ViewHolder> {

    private SparseBooleanArray selectedItems;

    private List<Call> original_items = new ArrayList<>();
    private List<Call> filtered_items = new ArrayList<>();
    private ItemFilter mFilter = new ItemFilter();

    private Context ctx;
    private boolean clicked = false;

    public CallsListAdapter(Context ctx,List<Call> items) {
        this.ctx = ctx;
        original_items = items;
        filtered_items = items;
        selectedItems = new SparseBooleanArray();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_calls, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Call c = filtered_items.get(position);
        holder.title.setText((c.getPhone()));
        if(c.getType().equals("PERSONAL")){
            holder.content.setText("PERSONAL CALL");
        }else{
            holder.content.setText(c.getCharge_code());
        }
        holder.duration.setText(c.getDuration());
        holder.time.setText(c.getStart());

        setAnimation(holder.itemView, position);

        holder.lyt_parent.setActivated(selectedItems.get(position, false));

        clicked = false;
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

    public List<Call> getSelectedItems() {
        List<Call> items = new ArrayList<>();
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(filtered_items.get(selectedItems.keyAt(i)));
        }
        return items;
    }

    @Override
    public int getItemCount() {
        return filtered_items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView title;
        public TextView content;
        public TextView time;
        public TextView duration;
        public ImageView image;
        public LinearLayout lyt_parent;

        public ViewHolder(View v) {
            super(v);
            title = (TextView) v.findViewById(R.id.title);
            content = (TextView) v.findViewById(R.id.content);
            time = (TextView) v.findViewById(R.id.time);
            duration = (TextView) v.findViewById(R.id.duration);
            image = (ImageView) v.findViewById(R.id.image);
            lyt_parent = (LinearLayout) v.findViewById(R.id.lyt_parent);
        }
    }

    public Filter getFilter() {
        return mFilter;
    }

    public void addItems(List<Call> calls) {
        this.original_items = calls;
        this.filtered_items = calls;
        notifyDataSetChanged();
    }

    private class ItemFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            String query = constraint.toString().toLowerCase();

            FilterResults results = new FilterResults();
            final List<Call> list = original_items;
            final List<Call> result_list = new ArrayList<>(list.size());

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
            filtered_items = (List<Call>) results.values;
            notifyDataSetChanged();
        }

    }

    private String fetchContactName(String phone){

        if(hasPermission(Manifest.permission.READ_CONTACTS)){
            return getContactName(phone,ctx);
        }else{
            return  phone;
        }

    }

    private boolean hasPermission(String permission){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return(ctx.checkSelfPermission(permission)== PackageManager.PERMISSION_GRANTED);
        }

        return true;

    }

    private String getContactName(final String phoneNumber, Context context) {
        Uri uri=Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(phoneNumber));

        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};

        String contactName = phoneNumber;
        Cursor cursor=context.getContentResolver().query(uri,projection,null,null,null);

        if (cursor != null) {
            if(cursor.moveToFirst()) {
                contactName=cursor.getString(0);
            }
            cursor.close();
        }

        return contactName;
    }
}
