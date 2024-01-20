package com.darktornado.cardbalancechecker;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcF;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.darktornado.library.FeliCa;
import com.darktornado.library.ICCard;

public class MainActivity extends Activity {

    private LinearLayout layout;
    private NfcAdapter adapter;
    private PendingIntent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layout = new LinearLayout(this);
        layout.setOrientation(1);

        TextView txt = new TextView(this);
        txt.setText("스마트폰의 NFC 기능을 기본 모드로 활성화시키고, 스마트폰 뒷면에 교통카드를 태그해주세요.\n\n" +
                "[사용 가능 카드 목록]\n" +
                " - 한국 : 티머니, 캐시비, 레일플러스\n" +
                " - 일본 : Suica 및 상호호환카드");
        layout.addView(txt);

        TextView maker = new TextView(this);
        maker.setText("\n© 2023-2024 Dark Tornado, All rights reserved.\n");
        maker.setTextSize(13);
        maker.setGravity(Gravity.CENTER);
        layout.addView(maker);

        int pad = dip2px(16);
        layout.setPadding(pad, pad, pad, pad);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);

        adapter = NfcAdapter.getDefaultAdapter(this);
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        this.intent = PendingIntent.getActivity(this, 0, intent, 0);
    }

    private void applyData(String type, String balance, String cardId, Bitmap bitmap) {
        layout.removeAllViews();

        ImageView image = new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (bitmap != null) image.setImageBitmap(bitmap);
        layout.addView(image);
        int pad = dip2px(16);
        image.setPadding(pad, pad, pad, pad);

        WebView web = new WebView(this);
        final StringBuilder result = new StringBuilder("<meta name='viewport' content='user-scalable=no width=device-width' />")
                .append("<style>table{border: 1px solid #000000;border-collapse: collapse;}td{padding:5px;font-size:18px}</style>")
                .append("<table width=100% border=1>")
                .append("<tr align=center><td><b>종류</b></td><td>").append(type).append("</td></tr>")
                .append("<tr align=center><td><b>잔액</b></td><td>").append(balance).append("</td></tr>")
                .append("<tr align=center><td><b>카드번호</b></td><td>").append(cardId).append("</td></tr>")
                .append("</table>");

        if (Build.VERSION.SDK_INT > 23) {
            web.loadDataWithBaseURL(null, result.toString(), "text/html; charset=UTF-8", null, null);
        } else {
            web.loadData(result.toString(), "text/html; charset=UTF-8", null);
        }
        web.setBackgroundColor(0);
        layout.addView(web);

        TextView maker = new TextView(this);
        maker.setText("\n© 2023-2024 Dark Tornado, All rights reserved.\n");
        maker.setTextSize(13);
        maker.setGravity(Gravity.CENTER);
        layout.addView(maker);
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
                Bitmap bitmap = null;
                switch (card.type) {  //근데, switch 사용해도 문자열 비교라서 어차피 컴파일하면 모든 case 안에 if문 들어감. (hashcode로 switch 실행 후 일치하면 if로 다시 비교)
                    case "티머니":
                        bitmap = BitmapFactory.decodeStream(getAssets().open("tmoney.png"));
                        break;
                    case "캐시비":
                        bitmap = BitmapFactory.decodeStream(getAssets().open("cashbee.png"));
                        break;
                    case "레일플러스":
                        bitmap = BitmapFactory.decodeStream(getAssets().open("railplus.png"));
                        break;
                }
                String cardId = card.number.substring(0, 4) + " " + card.number.substring(4, 8) + " " + card.number.substring(8, 12) + " " + card.number.substring(12, 16);
                applyData(card.type, card.balance+"원", cardId, bitmap);
            }
            else if (nf != null && tag.getId() != null) { //일본 교통카드
                FeliCa card = new FeliCa(nf, tag.getId());
                applyData("Suica 계열", card.balance+"원", card.id, BitmapFactory.decodeStream(getAssets().open("suica.png")));
            }
            else {
                toast("이 앱에서는 처리할 수 없는 카드에요");
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