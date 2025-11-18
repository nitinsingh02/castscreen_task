package com.example.celebrareproject;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TutorialPagerAdapter extends FragmentStateAdapter {

    private final int[] pageLayouts;

    // Constructor
    public TutorialPagerAdapter(@NonNull Fragment fragment, int[] pageLayouts) {
        super(fragment);
        this.pageLayouts = pageLayouts;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        int layoutRes = pageLayouts[position];
        return PageFragment.newInstance(layoutRes);  // FIXED
    }

    @Override
    public int getItemCount() {
        return pageLayouts.length;
    }
}
