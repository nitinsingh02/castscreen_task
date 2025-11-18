package com.example.celebrareproject;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class FAQFragment extends Fragment {

    public FAQFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_f_a_q, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewFAQ);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<FAQItem> faqList = getSampleFAQs();
        FAQAdapter adapter = new FAQAdapter(faqList);
        recyclerView.setAdapter(adapter);
    }

    private List<FAQItem> getSampleFAQs() {
        List<FAQItem> list = new ArrayList<>();

        // 8 sample FAQs — replace text as you like
        list.add(new FAQItem("My phone gets stuck at Connecting when pairing up with TV.",
                "-Restart the smartphone's Wi-Fi and see if the connection is successfull \n\n - Turn off the TV and socket as well. Wait 1 minute and restart the TV. \n\n - Shorten the distance between your phone and the TV. Take it up close"));
        list.add(new FAQItem("It worked for me before, but it doesn't work now.",
                "- if it worked before, then it definitely can be used again. \n\n - most connecting problems can be solved by restarting devices. Just restart both your phone and TV, follow the regular connection process to retry"));
        list.add(new FAQItem("It shows that my device is not supported.",
                "- This means that your phone itself does not support the screen mirroring function. \n\n - You can use XCast to cast video, audio and photo to TV. with XCast, you can do other things with your phone while casting to TV."));
        list.add(new FAQItem("My TV is an old Tv and does not support Wi-Fi.",
                "- If your TV supports HDMI interface, we recommend that you buy an external receiver for screen mirroring. Such as Chromecast, Anycast etc."));
        list.add(new FAQItem("TV has sound but no picture.",
                "- some content involves copyright protection and cannot be displayed on third-party devices, such as Netflix, Disney Plus etc. You can download the video or look for the same resource on other apps to have a try"));
        list.add(new FAQItem("Connection will be interrupted after a while.",
                "- You may turn on the Smart Connection in your Wi-fi settings. It will search for better Wi-Fi connection constantly, and always connect back and forth. \n\n - Please make sure that the Wi-Fi of the mobile phone and TV is the same and the best signal."));
        list.add(new FAQItem("Is my personal information or mobile data absolutely safe?",
                "-We will not monitor or collect any information on you and your mobile phone. You can cast your screen without any worry. \n\n - The system may warn you that "));
        return list;
    }
}
