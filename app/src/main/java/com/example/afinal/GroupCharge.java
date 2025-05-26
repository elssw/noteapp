// GroupCharge.java
package com.example.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.flexbox.FlexboxLayout;

import com.example.afinal.model.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class GroupCharge extends AppCompatActivity {
    private static final int MAX_LEN = 20;
    private TextView tvAmountDisplay;
    private TextView tvNote;
    private StringBuilder amountBuilder = new StringBuilder();
    private FlexboxLayout memberContainer;
    private List<String> allMembers = Arrays.asList("我", "A", "B");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_charge);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            );
            return insets;
        });

        tvAmountDisplay = findViewById(R.id.tvAmountDisplay);
        tvAmountDisplay.setText("NT$0");

        tvNote = findViewById(R.id.note);
        tvNote.setOnClickListener(v -> {
            TextView customTitle = new TextView(GroupCharge.this);
            customTitle.setText("備註");
            customTitle.setGravity(Gravity.CENTER);
            customTitle.setTextSize(20);
            EditText input = new EditText(GroupCharge.this);
            input.setHint("請輸入備註");
            new AlertDialog.Builder(GroupCharge.this)
                    .setCustomTitle(customTitle)
                    .setView(input)
                    .setPositiveButton("確定", (d, w) -> {
                        String text = input.getText().toString().trim();
                        if (!text.isEmpty()) tvNote.setText(text);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        RecyclerView rv = findViewById(R.id.items);
        rv.setLayoutManager(new GridLayoutManager(this, 4));
        rv.setAdapter(new CategoryAdapter(this, Arrays.asList(
                new Category(R.drawable.ic_group, "群組"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_setting, "設定")
        )));

        memberContainer = findViewById(R.id.memberSelectionContainer);
        for (String name : allMembers) {
            CheckBox cb = new CheckBox(this);
            cb.setText(name);
            cb.setChecked(true);
            memberContainer.addView(cb);
        }

        int[] inputIds = {
                R.id.number_1, R.id.number_2, R.id.number_3,
                R.id.number_4, R.id.number_5, R.id.number_6,
                R.id.number_7, R.id.number_8, R.id.number_9,
                R.id.number_0, R.id.point,
                R.id.plus, R.id.subtract, R.id.multiply, R.id.divide
        };
        for (int id : inputIds) findViewById(id).setOnClickListener(this::onDigitClick);

        findViewById(R.id.clear).setOnClickListener(this::onClearClick);
        findViewById(R.id.back).setOnClickListener(this::onBackClick);
        findViewById(R.id.equal).setOnClickListener(this::onEqualClick);

        findViewById(R.id.confirm).setOnClickListener(v -> {
            onEqualClick(v);
            doSplitBill();
        });
    }

    private void doSplitBill() {
        if (amountBuilder.length() == 0) {
            Toast.makeText(this, "請先輸入金額", Toast.LENGTH_SHORT).show();
            return;
        }

        double total;
        try {
            total = Double.parseDouble(amountBuilder.toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "金額格式錯誤", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> selected = new ArrayList<>();
        for (int i = 0; i < memberContainer.getChildCount(); i++) {
            CheckBox cb = (CheckBox) memberContainer.getChildAt(i);
            if (cb.isChecked()) selected.add(cb.getText().toString());
        }
        if (selected.isEmpty()) {
            Toast.makeText(this, "請至少選擇一位分帳對象", Toast.LENGTH_SHORT).show();
            return;
        }

        double each = total / selected.size();
        StringBuilder sb = new StringBuilder("總金額 NT$" + total + "\n\n");
        for (String name : selected) {
            if (name.equals("我")) {
                sb.append(name).append(" 先支付 NT$").append(total).append("\n");
            } else {
                sb.append(name).append(" 應付 NT$").append(each).append("\n");
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("分帳結果")
                .setMessage(sb.toString())
                .setPositiveButton("確認", (dialog, which) -> {
                    Intent result = new Intent();
                    result.putExtra("summary", sb.toString());
                    setResult(RESULT_OK, result);
                    finish();
                })
                .show();
    }

    public void onDigitClick(View v) {
        if (amountBuilder.length() >= MAX_LEN) return;
        String key = ((TextView) v).getText().toString();
        char c = key.charAt(0);
        if (isOperator(c)) {
            if (amountBuilder.length() == 0) return;
            char last = amountBuilder.charAt(amountBuilder.length() - 1);
            if (isOperator(last)) amountBuilder.setCharAt(amountBuilder.length() - 1, c);
            else amountBuilder.append(c);
        } else if (c == '.') {
            if (amountBuilder.length() == 0) amountBuilder.append("0.");
            else {
                char last = amountBuilder.charAt(amountBuilder.length() - 1);
                if (isOperator(last)) amountBuilder.append("0.");
                else {
                    int i = amountBuilder.length() - 1;
                    while (i >= 0 && !isOperator(amountBuilder.charAt(i))) i--;
                    String segment = amountBuilder.substring(i + 1);
                    if (!segment.contains(".")) amountBuilder.append('.');
                }
            }
        } else {
            if (c == '0' && amountBuilder.length() == 0) return;
            amountBuilder.append(c);
        }
        tvAmountDisplay.setText("NT$" + amountBuilder.toString());
    }

    public void onClearClick(View v) {
        amountBuilder.setLength(0);
        tvAmountDisplay.setText("NT$0");
    }

    public void onBackClick(View v) {
        if (amountBuilder.length() > 0) {
            amountBuilder.deleteCharAt(amountBuilder.length() - 1);
            tvAmountDisplay.setText("NT$" + (amountBuilder.length() > 0 ? amountBuilder : "0"));
        }
    }

    public void onEqualClick(View v) {
        try {
            while (amountBuilder.length() > 0 && (isOperator(amountBuilder.charAt(amountBuilder.length() - 1)) || amountBuilder.charAt(amountBuilder.length() - 1) == '.')) {
                amountBuilder.deleteCharAt(amountBuilder.length() - 1);
            }
            String expr = amountBuilder.toString().replace('×','*').replace('÷','/');
            double result = evaluate(expr);
            String resStr = formatResult(result);
            if (resStr.length() > MAX_LEN) resStr = resStr.substring(0, MAX_LEN);
            tvAmountDisplay.setText("NT$" + resStr);
            amountBuilder.setLength(0);
            amountBuilder.append(resStr);
        } catch (Exception e) {
            tvAmountDisplay.setText("Error");
            amountBuilder.setLength(0);
        }
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '×' || c == '÷';
    }

    private double evaluate(String expr) {
        Stack<Double> vals = new Stack<>();
        Stack<Character> ops = new Stack<>();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                int j = i;
                while (j < expr.length() && (Character.isDigit(expr.charAt(j)) || expr.charAt(j) == '.')) j++;
                vals.push(Double.parseDouble(expr.substring(i, j)));
                i = j;
            } else {
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(c)) applyOp(vals, ops.pop());
                ops.push(c);
                i++;
            }
        }
        while (!ops.isEmpty()) applyOp(vals, ops.pop());
        return vals.isEmpty() ? 0 : vals.pop();
    }

    private int precedence(char op) {
        return (op == '+' || op == '-') ? 1 : 2;
    }

    private void applyOp(Stack<Double> vals, char op) {
        double b = vals.pop();
        double a = vals.isEmpty() ? 0 : vals.pop();
        switch (op) {
            case '+': vals.push(a + b); break;
            case '-': vals.push(a - b); break;
            case '*': vals.push(a * b); break;
            case '/': vals.push(b != 0 ? a / b : 0); break;
        }
    }

    private String formatResult(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
