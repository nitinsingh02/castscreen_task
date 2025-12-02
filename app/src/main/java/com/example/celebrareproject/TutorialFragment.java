package com.example.celebrareproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;                // <- IMPORTANT
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * TutorialFragment with runtime language-change support.
 * It listens for broadcasts with action "com.example.celebrareproject.ACTION_LOCALE_CHANGED"
 * and refreshes its ViewPager pages and visible strings when received.
 */
public class TutorialFragment extends Fragment {

    private ViewPager2 viewPager;
    private TabLayout tabDots;
    private View ivNext;
    private View ivPrev;
    private Button btnSupported;

    // pages array - keep as before
    private final int[] pages = new int[] {
            R.layout.tutorial_page_1,
            R.layout.tutorial_page_2,
            R.layout.tutorial_page_3,
            R.layout.tutorial_page_4
    };

    private TutorialPagerAdapter adapter;
    private TabLayoutMediator tabMediator;

    // Use the same action string your settingPage broadcasts
    private static final String ACTION_LOCALE_CHANGED = "com.example.celebrareproject.ACTION_LOCALE_CHANGED";

    // Receiver that refreshes UI when language changes
    private final BroadcastReceiver localeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // run on UI thread
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                // re-create adapter & mediator so layouts are reinflated with new locale strings
                refreshAdapterAndTabs();
                // update any other view texts
                updateTexts();
            });
        }
    };

    public TutorialFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tutorial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // find views
        viewPager = view.findViewById(R.id.tutorialViewPager);
        tabDots = view.findViewById(R.id.tabDots);
        ivNext = view.findViewById(R.id.ivNext);
        ivPrev = view.findViewById(R.id.ivPrev);
        btnSupported = view.findViewById(R.id.btnSupported);

        // initial adapter & mediator setup
        adapter = new TutorialPagerAdapter(this, pages);
        viewPager.setAdapter(adapter);

        // Dots (TabLayoutMediator)
        tabMediator = new TabLayoutMediator(tabDots, viewPager, (tab, position) -> {
            tab.setIcon(R.drawable.dot_selector);
        });
        tabMediator.attach();

        // PageChange callback to set dot selected state
        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        for (int i = 0; i < tabDots.getTabCount(); i++) {
                            TabLayout.Tab tab = tabDots.getTabAt(i);
                            if (tab != null && tab.getCustomView() != null) {
                                tab.getCustomView().setSelected(i == position);
                            }
                        }
                    }
                }
        );

        // initial arrow visibility
        updateArrows(0, adapter.getItemCount(), ivPrev, ivNext);

        // arrow click listeners
        ivNext.setOnClickListener(v -> {
            int next = Math.min(viewPager.getCurrentItem() + 1, adapter.getItemCount() - 1);
            viewPager.setCurrentItem(next, true);
        });

        ivPrev.setOnClickListener(v -> {
            int prev = Math.max(viewPager.getCurrentItem() - 1, 0);
            viewPager.setCurrentItem(prev, true);
        });

        // update arrows on page change
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateArrows(position, adapter.getItemCount(), ivPrev, ivNext);
            }
        });

        // remove overscroll glow
        if (viewPager.getChildCount() > 0) {
            viewPager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        btnSupported.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity().getApplication(), supportDevice.class);
            startActivity(intent);
        });

        // set initial texts
        updateTexts();
    }

    @Override
    public void onStart() {
        super.onStart();
        // register receiver (defensive: requireContext may be null in rare cases)
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(requireContext())
                    .registerReceiver(localeReceiver, new IntentFilter(ACTION_LOCALE_CHANGED));
        }
    }

    @Override
    public void onStop() {
        // unregister receiver
        try {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(localeReceiver);
        } catch (Exception ignored) {}
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        // detach mediator to avoid leaks
        try {
            if (tabMediator != null) tabMediator.detach();
        } catch (Exception ignored) {}
        super.onDestroyView();
    }

    /**
     * Update any visible texts in this fragment that come from resources.
     * Always call this after a locale change so the UI reloads new strings.
     */
    private void updateTexts() {
        if (btnSupported != null && isAdded()) {
            btnSupported.setText(getString(R.string.supportDevice)); // example - change key if needed
        }
        // update other text views if you have them
    }

    /**
     * Re-create adapter & TabLayoutMediator so page layouts are reinflated
     * with the current locale resources.
     */
    private void refreshAdapterAndTabs() {
        // Detach old mediator if present
        try {
            if (tabMediator != null) {
                tabMediator.detach();
            }
        } catch (Exception ignored) {}

        // Create a fresh adapter and assign
        adapter = new TutorialPagerAdapter(this, pages);
        viewPager.setAdapter(adapter);

        // Create and attach new mediator
        tabMediator = new TabLayoutMediator(tabDots, viewPager, (tab, position) -> {
            tab.setIcon(R.drawable.dot_selector);
        });
        tabMediator.attach();

        // Reset arrows based on current item
        updateArrows(viewPager.getCurrentItem(), adapter.getItemCount(), ivPrev, ivNext);
    }

    private void updateArrows(int position, int pageCount, View ivPrev, View ivNext) {
        if (ivPrev != null) ivPrev.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
        if (ivNext != null) ivNext.setVisibility(position == pageCount - 1 ? View.GONE : View.VISIBLE);
    }
}
