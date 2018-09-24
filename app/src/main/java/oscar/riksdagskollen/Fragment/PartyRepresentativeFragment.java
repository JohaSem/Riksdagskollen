package oscar.riksdagskollen.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.VolleyError;

import java.util.ArrayList;
import java.util.List;

import oscar.riksdagskollen.Activity.RepresentativeDetailActivity;
import oscar.riksdagskollen.RiksdagskollenApp;
import oscar.riksdagskollen.Util.Adapter.RepresentativeAdapter;
import oscar.riksdagskollen.Util.Adapter.RiksdagenViewHolderAdapter;
import oscar.riksdagskollen.Util.Callback.RepresentativeListCallback;
import oscar.riksdagskollen.Util.JSONModel.Party;
import oscar.riksdagskollen.Util.JSONModel.Representative;


/**
 * Created by oscar on 2018-03-29.
 */

public class PartyRepresentativeFragment extends RiksdagenAutoLoadingListFragment {

    private final List<Representative> representativeList = new ArrayList<>();
    private RepresentativeAdapter adapter;
    private Party party;

    public static PartyRepresentativeFragment newInstance(Party party) {
        Bundle args = new Bundle();
        args.putParcelable("party", party);
        PartyRepresentativeFragment newInstance = new PartyRepresentativeFragment();
        newInstance.setArguments(args);
        return newInstance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.party = getArguments().getParcelable("party");
        return super.onCreateView(inflater, container, savedInstanceState);
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new RepresentativeAdapter(representativeList, new RiksdagenViewHolderAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Object document) {
                Intent repDetailsIntent = new Intent(getContext(), RepresentativeDetailActivity.class);
                repDetailsIntent.putExtra("representative", (Representative) document);
                startActivity(repDetailsIntent);
            }
        });

    }

    /**
     * Load the next page and add it to the adapter when downloaded and parsed.
     * Hides the loading view.s
     */
    protected void loadNextPage() {
        setLoadingMoreItems(true);
        RiksdagskollenApp.getInstance().getRiksdagenAPIManager().getRepresentativesInParty(party.getID(), new RepresentativeListCallback() {
            @Override
            public void onPersonListFetched(List<Representative> representatives) {
                setShowLoadingView(false);
                representativeList.addAll(representatives);
                getAdapter().addAll(representatives);
                setLoadingMoreItems(false);
            }

            @Override
            public void onFail(VolleyError error) {
                setLoadingMoreItems(false);
                decrementPage();
            }
        });
        incrementPage();
    }

    @Override
    RiksdagenViewHolderAdapter getAdapter() {
        return adapter;
    }
}