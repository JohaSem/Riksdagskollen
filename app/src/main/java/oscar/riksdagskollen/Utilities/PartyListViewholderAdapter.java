package oscar.riksdagskollen.Utilities;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import oscar.riksdagskollen.R;
import oscar.riksdagskollen.Utilities.JSONModels.PartyDocument;

/**
 * Created by shelbot on 2018-03-27.
 */

public class PartyListViewholderAdapter extends RecyclerView.Adapter<PartyListViewholderAdapter.MyViewHolder> {
    private List<PartyDocument> documentList;
    private Context context;

    public class MyViewHolder extends RecyclerView.ViewHolder{
        public TextView document;
        public MyViewHolder(View partyView) {
            super(partyView);
            document = (TextView) partyView.findViewById(R.id.document);
            partyView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                }
            });
        }
    }



    public PartyListViewholderAdapter(Context context, List<PartyDocument> documentList) {
        this.documentList = documentList;
        this.context =  context;
    }



    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.party_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    public void setDocumentList(List<PartyDocument> documentList) {
        this.documentList = documentList;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        PartyDocument document = documentList.get(position);
        holder.document.setText(document.getTitel());
    }

    @Override
    public int getItemCount() {
        return documentList.size();
    }


}
