package oscar.riksdagskollen.Fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import oscar.riksdagskollen.BuildConfig;
import oscar.riksdagskollen.R;
import oscar.riksdagskollen.RiksdagskollenApp;
import oscar.riksdagskollen.Util.Helper.CustomTabLayout;
import oscar.riksdagskollen.Util.JSONModel.Party;

/**
 * Created by oscar on 2018-08-28.
 * This fragment holds 3 other fragments in tabs.
 */
public class PartyFragment extends Fragment {

    private Party party;
    private PartyListFragment listFragment;
    private PartyInfoFragment infoFragment;
    private PartyRepresentativeFragment representativeFragment;
    private TwitterListFragment twitterListFragment;
    private CustomTabLayout tabLayout;
    private ViewPager viewPager;
    private int currentPage = 0;
    private int alpha = 255;
    private MenuItem notif;
    private MenuItem filter;
    private Menu menu;

    private static final String TAG = "Partyfragment";


    public static PartyFragment newInstance(Party party){
        Bundle args = new Bundle();
        args.putParcelable("party",party);
        PartyFragment newInstance = new PartyFragment();
        newInstance.setArguments(args);
        return newInstance;
    }


    @Override
    public void onDetach() {
        tabLayout.setVisibility(View.GONE);
        restoreMenuItems();
        super.onDetach();
    }

    private void restoreMenuItems() {
        if (filter != null) {
            filter.getIcon().setAlpha(255);
            filter.setEnabled(true);
        }

        if (notif != null) {
            notif.getIcon().setAlpha(255);
            notif.setEnabled(true);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.party = getArguments().getParcelable("party");
        infoFragment = PartyInfoFragment.newInstance(party);
        representativeFragment = PartyRepresentativeFragment.newInstance(party);
        twitterListFragment = TwitterListFragment.newInstance(party);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(party.getName());
        View view = inflater.inflate(R.layout.fragment_party,container, false);
        // Setting ViewPager for each Tabs
        viewPager = view.findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(4);
        setupViewPager();
        // Set Tabs inside Toolbar
        tabLayout = getActivity().findViewById(R.id.result_tabs);
        tabLayout.setVisibility(View.VISIBLE);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        tabLayout.setupWithViewPager(viewPager);
        return view;

    }


    public void setListFragment(PartyListFragment listFragment) {
        this.listFragment = listFragment;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        updateMenuItemAlpha(0);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.party_feed_menu, menu);
        this.menu = menu;
        if (RiksdagskollenApp.getInstance().getAlertManager().isAlertEnabledForParty(party.getID())) {
            menu.findItem(R.id.notification_menu_item).setIcon(R.drawable.ic_notification_enabled);
        }
        this.filter = menu.findItem(R.id.menu_filter);
        this.notif = menu.findItem(R.id.notification_menu_item);
        super.onCreateOptionsMenu(menu, inflater);
    }

    // Add Fragments to Tabs
    private void setupViewPager() {
        Adapter adapter = new Adapter(getChildFragmentManager());
        adapter.addFragment(listFragment, "Flöde");
        adapter.addFragment(infoFragment, "Parti");
        adapter.addFragment(representativeFragment,"Ledamöter");
        // Only have twitter on play version
        if (BuildConfig.FLAVOR.equalsIgnoreCase("play")) {
            adapter.addFragment(twitterListFragment, "Twitter");
        }

        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                currentPage = position;
                updateMenuItemAlpha(positionOffset);
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }


    private void updateMenuItemAlpha(float positionOffset) {
        if (menu == null) return;

        if (notif != null && filter != null) {
            int newAlpha = (int) (255 - positionOffset * 255 - 255 * currentPage);
            if (newAlpha < 0) newAlpha = 0;

            //Detect a visual bug where difference is too big
            if (Math.abs(newAlpha - alpha) > 200) newAlpha = alpha;

            alpha = newAlpha;
            filter.getIcon().setAlpha(alpha);
            notif.getIcon().setAlpha(alpha);
            if (currentPage > 0 && notif.isEnabled() && filter.isEnabled()) {
                filter.getIcon().setAlpha(0);
                notif.getIcon().setAlpha(0);
                notif.setEnabled(false);
                filter.setEnabled(false);
            } else if (currentPage == 0 && !notif.isEnabled() && !filter.isEnabled()) {
                notif.setEnabled(true);
                filter.setEnabled(true);
            } else if (currentPage > 0) {
                filter.getIcon().setAlpha(0);
                notif.getIcon().setAlpha(0);
            }
        }
    }


    static class Adapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public Adapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }


}
