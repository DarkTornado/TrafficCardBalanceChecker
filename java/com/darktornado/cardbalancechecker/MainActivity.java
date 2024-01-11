package com.darktornado.cardbalancechecker;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.darktornado.library.ICCard;

public class MainActivity extends AppCompatActivity {

    private LinearLayout layout;
    private NfcAdapter adapter;
    private PendingIntent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layout = new LinearLayout(this);
        layout.setOrientation(1);

        TextView txt = new TextView(this);
        txt.setText("스마트폰의 NFC 기능을 활성화시키고, 스마트폰 뒷면에 교통카드를 태그해주세요.\n\n" +
                "[사용 가능 카드 목록]\n" +
                " - 한국 : 티머니, 캐시비, 레일플러스\n" +
                " - 일본 : Suica 및 상호호환카드");
        layout.addView(txt);

        int pad = dip2px(16);
        layout.setPadding(pad, pad, pad, pad);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);

        adapter = NfcAdapter.getDefaultAdapter(this);
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        this.intent = PendingIntent.getActivity(this, 0, intent, 0);
    }

    private void applyData(ICCard card) {
        layout.removeAllViews();
        ImageView image = new ImageView(this);
        layout.addView(image);
        TextView txt = new TextView(this);
        txt.setText("종류 : " + card.type + "\n" +
                "잔액 : " + card.balance + "원\n" +
                "카드번호 : " + card.number);
        layout.addView(txt);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        try {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag == null) return;

            NfcF nf = NfcF.get(tag);
            IsoDep id = IsoDep.get(tag);

            if (id != null) { //한국 교통카드
                ICCard card = new ICCard(id);
                applyData(card);
            }
            else if (nf != null && tag.getId() != null) { //일본 교통카드
//                FeliCa fc = new FeliCa(nf, tag.getId());
//                applyResultJP(fc);
            }
            else {
                toast("아직 처리할 수 없는 카드에요");
            }

        } catch (Exception e) {
            toast(e.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) adapter.enableForegroundDispatch(this, intent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adapter != null) adapter.disableForegroundDispatch(this);
    }


    private void toast(final String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private int dip2px(int dips) {
        return (int) Math.ceil(dips * getResources().getDisplayMetrics().density);
    }
}