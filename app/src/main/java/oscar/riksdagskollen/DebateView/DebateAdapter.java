package oscar.riksdagskollen.DebateView;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import oscar.riksdagskollen.Activity.RepresentativeDetailActivity;
import oscar.riksdagskollen.DebateView.Data.DebateStatement;
import oscar.riksdagskollen.DebateView.Data.Speech;
import oscar.riksdagskollen.R;
import oscar.riksdagskollen.RepresentativeList.data.Representative;
import oscar.riksdagskollen.RiksdagskollenApp;
import oscar.riksdagskollen.Util.Adapter.RiksdagenViewHolderAdapter;
import oscar.riksdagskollen.Util.Enum.CurrentParties;
import oscar.riksdagskollen.Util.RiksdagenCallback.RepresentativeCallback;
import oscar.riksdagskollen.Util.View.CircularImageView;

public class DebateAdapter extends RiksdagenViewHolderAdapter {

    private Context context;
    private ArrayList<DebateStatement> debateStatements;
    private String debateInitiatior;
    private boolean showPlayLabel = false;

    private static final int TYPE_OUTGOING = 444;
    private static final int TYPE_INCOMING = 555;

    private WebTvListener webTvListener;

    DebateAdapter(Context context, ArrayList<DebateStatement> speeches, String debateInitiator) {
        super(new OnItemClickListener() {
            @Override
            public void onItemClick(Object document) {

            }
        });
        this.debateStatements = speeches;
        this.context = context;
        this.debateInitiatior = debateInitiator;
    }

    public void setShowPlayLabel(boolean showPlayLabel) {
        this.showPlayLabel = showPlayLabel;
    }

    public void setWebTvListener(WebTvListener webTvListener) {
        this.webTvListener = webTvListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        if (viewType == TYPE_INCOMING) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.speech_item_incoming, parent, false);
            return new DebateViewHolderItem(itemView, context);

        } else if (viewType == TYPE_OUTGOING) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.speech_item_outgoing, parent, false);
            return new DebateViewHolderItem(itemView, context);
        } else {
            FrameLayout frameLayout = new FrameLayout(parent.getContext());
            //make sure it fills the space
            frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new HeaderFooterViewHolder(frameLayout);
        }
    }

    void setSpeechDetail(Speech speech, String anf) {
        for (int i = 0; i < debateStatements.size(); i++) {
            if (debateStatements.get(i).getAnf_nummer().equals(anf)) {

                debateStatements.get(i).setSpeech(speech);
                notifyItemChanged(i + headers.size());
                break;
            }
        }

    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {

        if (position < headers.size()) {
            View v = headers.get(position);
            //add our view to a header view and display it
            prepareHeaderFooter((HeaderFooterViewHolder) holder, v);
        } else if (position >= headers.size() + debateStatements.size()) {
            View v = footers.get(position - debateStatements.size() - headers.size());
            //add our view to a footer view and display it
            prepareHeaderFooter((HeaderFooterViewHolder) holder, v);
        } else {
            final DebateViewHolderItem debateView = (DebateViewHolderItem) holder;
            DebateStatement debateStatement = debateStatements.get(position - headers.size());
            debateView.bind(debateStatement);
        }
    }


    @Override
    public int getItemViewType(int position) {
        //check what type our position is, based on the assumption that the order is headers > sortedList > footers
        if (position < headers.size()) {
            return TYPE_HEADER;
        } else if (position >= headers.size() + debateStatements.size()) {
            return TYPE_FOOTER;
        }

        if (debateStatements.get(position - headers.size()).getIntressent_id().equals(debateInitiatior)) {
            return TYPE_OUTGOING;
        } else if (debateInitiatior != null) {
            return TYPE_INCOMING;
        } else if (position % 2 == 0) {
            return TYPE_OUTGOING;
        }
        return TYPE_INCOMING;
    }


    @Override
    public int getItemCount() {
        return debateStatements.size();
    }

    @Override
    public void replaceAll(List<?> items) {

    }

    @Override
    public void addAll(List<?> items) {

    }

    @Override
    public void removeAll(List<?> items) {

    }

    class DebateViewHolderItem extends RecyclerView.ViewHolder {
        TextView speakerName;
        TextView time;
        TextView speech;
        TextView playLabel;

        CircularImageView portrait;
        ImageView partyLogo;
        Context context;
        ProgressBar loadingView;
        LinearLayout speechInfoView;


        DebateViewHolderItem(View itemView, Context context) {
            super(itemView);
            this.context = context;
            speakerName = itemView.findViewById(R.id.debate_item_speaker);
            time = itemView.findViewById(R.id.debate_item_time);
            speech = itemView.findViewById(R.id.debate_item_text);
            loadingView = itemView.findViewById(R.id.debate_item_loading_view);
            speechInfoView = itemView.findViewById(R.id.debate_item_info);
            portrait = itemView.findViewById(R.id.debate_item_portrait);
            partyLogo = itemView.findViewById(R.id.debate_item_portrait_party_logo);
            playLabel = itemView.findViewById(R.id.play_statement);
        }


        void bind(final DebateStatement debateStatement) {
            speakerName.setText(debateStatement.getTalare());
            time.setText(debateStatement.getAnf_klockslag());
            int drawableResource = CurrentParties.getParty(debateStatement.getParti()).getDrawableLogo();
            partyLogo.setImageResource(drawableResource);
            setLoading(true);
            speakerName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
            Speech speechDetail = debateStatement.getSpeech();
            if (speechDetail != null) {
                setLoading(false);
                speech.setText(Html.fromHtml(cleanupSpeech(speechDetail.getAnforandetext())));
            }
            int visibility = showPlayLabel ? View.VISIBLE : View.GONE;
            playLabel.setVisibility(visibility);

            if (showPlayLabel) {
                playLabel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (webTvListener != null) {
                            String start = debateStatement.getVideo_url().split("pos=")[1];
                            try {
                                int startSec = Integer.parseInt(start);
                                webTvListener.onPlayLabelPressed(startSec);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }

            RiksdagskollenApp.getInstance().getRiksdagenAPIManager()
                    .getRepresentative(debateStatement.getIntressent_id(), new RepresentativeCallback() {
                        @Override
                        public void onPersonFetched(final Representative representative) {

                            Glide
                                    .with(context.getApplicationContext())
                                    .load(representative.getBild_url_80())
                                    .into(portrait);
                            portrait.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent repDetailsIntent = new Intent(context, RepresentativeDetailActivity.class);
                                    repDetailsIntent.putExtra("representative", representative);
                                    context.startActivity(repDetailsIntent);
                                }
                            });

                            speakerName.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent repDetailsIntent = new Intent(context, RepresentativeDetailActivity.class);
                                    repDetailsIntent.putExtra("representative", representative);
                                    context.startActivity(repDetailsIntent);
                                }
                            });
                        }

                        @Override
                        public void onFail(VolleyError error) {

                        }
                    });
        }

        void setLoading(boolean loading) {
            if (loading) {
                speechInfoView.setVisibility(View.GONE);
                speech.setVisibility(View.GONE);
                loadingView.setVisibility(View.VISIBLE);
            } else {
                speechInfoView.setVisibility(View.VISIBLE);
                speech.setVisibility(View.VISIBLE);
                loadingView.setVisibility(View.GONE);
            }
        }

        private String cleanupSpeech(String speech) {
            speech = speech.replaceAll("STYLEREF Kantrubrik \\\\\\* MERGEFORMAT Svar på interpellationer", "");
            return speech;
        }


    }

    interface WebTvListener {
        void onPlayLabelPressed(int second);
    }


}
