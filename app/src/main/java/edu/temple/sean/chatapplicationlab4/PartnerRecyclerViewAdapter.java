package edu.temple.sean.chatapplicationlab4;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class PartnerRecyclerViewAdapter extends RecyclerView.Adapter<PartnerRecyclerViewAdapter.ViewHolder> {

    private LayoutInflater inflater;
    private Context context;
    private ArrayList<Partner> partners;

    public PartnerRecyclerViewAdapter(Context context, ArrayList<Partner> partners) {
        inflater = LayoutInflater.from(context);
        this.context = context;
        this.partners = partners;
    }

    public void setPartners(ArrayList<Partner> newList){
        partners = newList;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        final View root = inflater.inflate(R.layout.fragment_partner, viewGroup, false);
        root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                //int itemPosition = root.getChildLayoutPosition(view);
                //String item = mList.get(itemPosition);
            }
        });
        ViewHolder holder = new ViewHolder(root);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.name.setText(partners.get(i).getName());
    }

    @Override
    public int getItemCount() {
        return partners.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;

        ViewHolder(View itemView){
            super(itemView);
            name = (TextView)itemView.findViewById(R.id.name);
        }


    }
}
