package com.example.celebrareproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class TutorialFragment extends Fragment {

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
        ViewPager2 viewPager = view.findViewById(R.id.tutorialViewPager);
        TabLayout tabDots = view.findViewById(R.id.tabDots);
        View ivNext = view.findViewById(R.id.ivNext);
        View ivPrev = view.findViewById(R.id.ivPrev);
        Button btnSupported = view.findViewById(R.id.btnSupported);

        int[] pages = new int[] {
                R.layout.tutorial_page_1,
                R.layout.tutorial_page_2,
                R.layout.tutorial_page_3,
                R.layout.tutorial_page_4
        };

        TutorialPagerAdapter adapter = new TutorialPagerAdapter(this, pages);
        viewPager.setAdapter(adapter);

        // Dots
        new TabLayoutMediator(tabDots, viewPager, (tab, position) -> {
            tab.setIcon(R.drawable.dot_selector);
        }).attach();

        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {

                        // LOOP ALL DOTS & SET SELECTED STATE
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
        viewPager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);

        btnSupported.setOnClickListener(v -> {
            // TODO: open supported devices screen
        });
    }

    private void updateArrows(int position, int pageCount, View ivPrev, View ivNext) {
        ivPrev.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
        ivNext.setVisibility(position == pageCount - 1 ? View.GONE : View.VISIBLE);
    }
}
