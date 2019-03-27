package edu.temple.sean.chatapplicationlab4;


import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import edu.temple.sean.chatapplicationlab4.chat.ChatActivity;

public class PartnerRecyclerViewAdapter extends RecyclerView.Adapter<PartnerRecyclerViewAdapter.ViewHolder> {

    private LayoutInflater inflater;
    private Context context;
    private ArrayList<Partner> partners;
    private OnPartnerClickedListener mListener;

    public PartnerRecyclerViewAdapter(Context context, ArrayList<Partner> partners,
                                      OnPartnerClickedListener listener) {
        inflater = LayoutInflater.from(context);
        this.context = context;
        this.partners = partners;
        mListener = listener;
    }

    public void setPartners(ArrayList<Partner> newList){
        partners = newList;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        final View root = inflater.inflate(R.layout.fragment_partner, viewGroup, false);
        ViewHolder holder = new ViewHolder(root);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
        viewHolder.name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onPartnerClicked(viewHolder.getAdapterPosition());
            }
        });
        viewHolder.name.setText(partners.get(i).getName());
    }

    @Override
    public int getItemCount() {
        return partners.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        TextView name;

        ViewHolder(View itemView){
            super(itemView);
            name = (TextView)itemView.findViewById(R.id.name);
        }



    }

    public interface OnPartnerClickedListener {
        void onPartnerClicked(int position);
    }
}
