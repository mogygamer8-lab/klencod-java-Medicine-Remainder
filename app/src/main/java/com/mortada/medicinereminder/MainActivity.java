
package com.mortada.medicinereminder;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {

    LinearLayout mainLayout, medicinesLayout;
    ArrayList<JSONObject> medicines = new ArrayList<>();
    SharedPreferences prefs;

    int nextId = 1;

    final int BG = Color.rgb(15, 20, 29);
    final int CARD = Color.rgb(27, 35, 47);
    final int TEAL = Color.rgb(0, 210, 175);
    final int WHITE = Color.WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("MedicineData", MODE_PRIVATE);

        nextId = prefs.getInt("nextId", 1);

        createNotificationChannel();
        requestNotificationPermission();
        loadMedicines();
        buildScreen();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private TextView makeText(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private void buildScreen() {

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(dp(20), dp(28), dp(20), dp(25));
        mainLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        scroll.addView(mainLayout);

        TextView title = makeText("💊 تذكير الأدوية", 27, WHITE);
        title.setTypeface(null, Typeface.BOLD);
        mainLayout.addView(title);

        TextView subtitle = makeText(
            "لا تنسَ مواعيد أدويتك اليومية",
            14, Color.LTGRAY
        );
        subtitle.setPadding(0, dp(8), 0, dp(25));
        mainLayout.addView(subtitle);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView section = makeText("أدويتي", 21, WHITE);
        section.setTypeface(null, Typeface.BOLD);

        header.addView(section, new LinearLayout.LayoutParams(
            0, dp(50), 1
        ));

        Button addButton = new Button(this);
        addButton.setText("+ إضافة دواء");
        addButton.setTextColor(Color.BLACK);
        addButton.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(TEAL)
        );
        addButton.setOnClickListener(v -> showAddDialog());

        header.addView(addButton);

        mainLayout.addView(header);

        TextView info = makeText(
            "تظهر هنا قائمة الأدوية ومواعيدها",
            13, Color.LTGRAY
        );
        info.setPadding(0, dp(12), 0, dp(15));
        mainLayout.addView(info);

        medicinesLayout = new LinearLayout(this);
        medicinesLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.addView(medicinesLayout);

        refreshMedicines();

        setContentView(scroll);
    }

    private void showAddDialog() {

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(10), dp(20), dp(10));

        EditText nameInput = new EditText(this);
        nameInput.setHint("اسم الدواء");
        nameInput.setSingleLine(true);
        form.addView(nameInput);

        EditText doseInput = new EditText(this);
        doseInput.setHint("الجرعة (مثلاً: حبة واحدة)");
        doseInput.setSingleLine(true);
        form.addView(doseInput);

        TextView timeText = makeText("وقت التذكير: 08:00", 16, TEAL);
        timeText.setPadding(0, dp(18), 0, dp(10));
        form.addView(timeText);

        final int[] selectedHour = {8};
        final int[] selectedMinute = {0};

        Button timeButton = new Button(this);
        timeButton.setText("اختيار الوقت");

        timeButton.setOnClickListener(v -> {
            new TimePickerDialog(
                this,
                (picker, hour, minute) -> {
                    selectedHour[0] = hour;
                    selectedMinute[0] = minute;

                    timeText.setText(
                        String.format(Locale.getDefault(),
                        "وقت التذكير: %02d:%02d", hour, minute)
                    );
                },
                selectedHour[0],
                selectedMinute[0],
                true
            ).show();
        });

        form.addView(timeButton);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("إضافة دواء جديد")
            .setView(form)
            .setPositiveButton("حفظ", null)
            .setNegativeButton("إلغاء", null)
            .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {

                    String name = nameInput.getText().toString().trim();
                    String dose = doseInput.getText().toString().trim();

                    if (name.isEmpty()) {
                        nameInput.setError("أدخل اسم الدواء");
                        return;
                    }

                    if (dose.isEmpty()) {
                        doseInput.setError("أدخل الجرعة");
                        return;
                    }

                    JSONObject medicine = new JSONObject();

                    try {
                        int id = nextId++;

                        medicine.put("id", id);
                        medicine.put("name", name);
                        medicine.put("dose", dose);
                        medicine.put("hour", selectedHour[0]);
                        medicine.put("minute", selectedMinute[0]);

                        medicines.add(medicine);

                        saveMedicines();
                        scheduleReminder(medicine);
                        refreshMedicines();

                        Toast.makeText(this,
                            "تمت إضافة الدواء بنجاح 💊",
                            Toast.LENGTH_SHORT).show();

                        dialog.dismiss();

                    } catch (Exception e) {
                        Toast.makeText(this,
                            "حدث خطأ أثناء الحفظ",
                            Toast.LENGTH_SHORT).show();
                    }
                });
        });

        dialog.show();
    }

    private void refreshMedicines() {

        if (medicinesLayout == null) return;

        medicinesLayout.removeAllViews();

        if (medicines.isEmpty()) {
            TextView empty = makeText(
                "لا توجد أدوية مضافة حالياً\nاضغط على إضافة دواء للبدء.",
                16, Color.LTGRAY
            );

            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(55), 0, dp(55));

            medicinesLayout.addView(empty);
            return;
        }

        for (JSONObject medicine : medicines) {

            int id = medicine.optInt("id");
            String name = medicine.optString("name");
            String dose = medicine.optString("dose");
            int hour = medicine.optInt("hour");
            int minute = medicine.optInt("minute");

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(18), dp(15), dp(18), dp(15));
            card.setBackgroundColor(CARD);

            LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                    -1, -2
                );

            cardParams.bottomMargin = dp(12);
            card.setLayoutParams(cardParams);

            TextView medicineName = makeText(
                "💊  " + name, 19, WHITE
            );
            medicineName.setTypeface(null, Typeface.BOLD);

            card.addView(medicineName);

            TextView dosage = makeText(
                "الجرعة: " + dose, 15, Color.LTGRAY
            );
            dosage.setPadding(0, dp(10), 0, dp(5));
            card.addView(dosage);

            TextView time = makeText(
                "⏰  " + String.format(
                    Locale.getDefault(), "%02d:%02d", hour, minute
                ),
                17, TEAL
            );

            card.addView(time);

            Button deleteButton = new Button(this);
            deleteButton.setText("حذف الدواء");
            deleteButton.setTextColor(Color.WHITE);
            deleteButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                    Color.rgb(180, 55, 65)
                )
            );

            deleteButton.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                    .setTitle("حذف الدواء")
                    .setMessage("هل تريد حذف " + name + "؟")
                    .setPositiveButton("حذف", (d, w) -> {
                        cancelReminder(id);
                        removeMedicine(id);
                    })
                    .setNegativeButton("إلغاء", null)
                    .show();
            });

            card.addView(deleteButton);
            medicinesLayout.addView(card);
        }
    }

    private void removeMedicine(int id) {

        for (int i = 0; i < medicines.size(); i++) {
            if (medicines.get(i).optInt("id") == id) {
                medicines.remove(i);
                break;
            }
        }

        saveMedicines();
        refreshMedicines();
    }

    private void saveMedicines() {

        JSONArray array = new JSONArray();

        for (JSONObject medicine : medicines) {
            array.put(medicine);
        }

        prefs.edit()
            .putString("medicines", array.toString())
            .putInt("nextId", nextId)
            .apply();
    }

    private void loadMedicines() {

        String data = prefs.getString("medicines", "[]");

        try {
            JSONArray array = new JSONArray(data);

            for (int i = 0; i < array.length(); i++) {
                JSONObject medicine = array.getJSONObject(i);
                medicines.add(medicine);
            }

        } catch (Exception e) {
            medicines.clear();
        }
    }

    private void scheduleReminder(JSONObject medicine) {

        int id = medicine.optInt("id");

        Intent intent = new Intent(this, ReminderReceiver.class);

        intent.putExtra("id", id);
        intent.putExtra("name", medicine.optString("name"));
        intent.putExtra("dose", medicine.optString("dose"));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT |
            PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, medicine.optInt("hour"));
        calendar.set(Calendar.MINUTE, medicine.optInt("minute"));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        AlarmManager alarmManager =
            (AlarmManager) getSystemService(ALARM_SERVICE);

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.getTimeInMillis(),
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        );
    }

    private void cancelReminder(int id) {

        Intent intent = new Intent(this, ReminderReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT |
            PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
            (AlarmManager) getSystemService(ALARM_SERVICE);

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel = new NotificationChannel(
                "medicine_channel",
                "تذكير الأدوية",
                NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("تنبيهات مواعيد الأدوية");

            NotificationManager manager =
                getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private void requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                "android.permission.POST_NOTIFICATIONS"
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                new String[]{
                    "android.permission.POST_NOTIFICATIONS"
                },
                101
            );
        }
    }
}
