package com.example.khoan.myhomiee;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
// vi du can pass data to this ??

public class ChartTemp extends Fragment {
    Button btnSelectDay, btnSelectHour,btnViewchart;
    Spinner spinner;
    TextView txtInfo;
    private int mode = 0;
    Context context;
    private long dateMl = 0;
    private long hmMl =0;
    final int HOUR = 0;
    final int DAY  = 1;
    final int WEEK = 2;
    final int MONTH = 3;
    // co cach roi ne : dau tien nen get tu mainActivity  => ra duoc dung roi do :
    SimpleDateFormat formatter = null;
    CombinedChart mChart;
    List<String> times=null;
    int[] temps = null;
    View view;


    //String pathRoom = "/Room/1";
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.chart_temp_layout,container,false);
        btnSelectDay = view.findViewById(R.id.btnSelectDay);
        btnViewchart = view.findViewById(R.id.btnDrawChart);
        txtInfo = view.findViewById(R.id.txtInfo);
        dateMl = System.currentTimeMillis() - 1000*3600;
        hmMl = 0;

        btnViewchart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setChart();
            }
        });
        spinner =view.findViewById(R.id.spinnerMode);
        context = this.getContext();
        String[] options = new String[]{"hour","day","week"};
        ArrayAdapter<String> adapter= new ArrayAdapter<>(context,R.layout.support_simple_spinner_dropdown_item,options);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mode = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        btnSelectHour = view.findViewById(R.id.btnSelectHour);
        // co ban thi can xem lai ve git dung vay :
        btnSelectDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectDay();
            }
        });
        btnSelectHour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectHour();
            }
        });
        return view;

    }
    void buildChart(){
        mChart = (CombinedChart) view.findViewById(R.id.tempChart);
        mChart.getDescription().setEnabled(false);
        mChart.setBackgroundColor(Color.WHITE);
        mChart.setDrawGridBackground(false);
        mChart.setDrawBarShadow(false);
        mChart.setHighlightFullBarEnabled(false);
        mChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                Toast.makeText(context, "Value: "
                        + e.getY()
                        + ", index: "
                        + h.getX()
                        + ", DataSet index: "
                        + h.getDataSetIndex(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected() {

            }
        });

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setDrawGridLines(false);
        rightAxis.setAxisMinimum(0f);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);
        // dau tien rut gon time stamp => vi du :

        final List<String> xLabel = times;


        XAxis xAxis = mChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMinimum(0f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return xLabel.get((int) value % xLabel.size());
            }
        });


        CombinedData data = new CombinedData();
        LineData lineDatas = new LineData();

        lineDatas.addDataSet((ILineDataSet) setDataChart(temps));
        Log.d("xmax ", String.valueOf(data.getXMax()));

        data.setData(lineDatas);
        xAxis.setAxisMaximum(data.getXMax() + 0.25f);

        mChart.setData(data);
        mChart.invalidate();
    }

    void setChart(){
        Log.d("HELLO", "ready to set chart");
        final long chooseTime = dateMl + hmMl;
        Timestamp t1 = new Timestamp(chooseTime-1000);
        Log.d("DDD",new Date(t1.getTime()).toString());
        Timestamp t2=t1; long endTime=0;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference tempsRef = db.collection("TemperatureSensor");
        switch (mode){
            case HOUR:
                endTime = chooseTime + 1000*3600;
                t2= new Timestamp(endTime);
                break;
            case DAY:
                endTime = chooseTime + 1000*3600*24;
                t2 =new Timestamp(endTime);
                break;
            case WEEK:
                endTime = chooseTime + 1000*3600*24*7;
                t2 =new Timestamp(endTime);
                break;

        }
        SimpleDateFormat formatter =new SimpleDateFormat("dd-M-yyyy hh:mm");
        String sd= formatter.format(new Date(chooseTime)) ,ed = formatter.format(new Date(endTime));

        txtInfo.setText("Start: "+ sd +" to: "+ ed);

        String pathRoom = ((MainActivity)getActivity()).pathRoom;
        DocumentReference room1Ref= db.document(pathRoom);
        Log.d("III",room1Ref.getId());
        tempsRef.whereGreaterThanOrEqualTo("time",t1).whereLessThanOrEqualTo("time",t2).whereEqualTo("FkSensor_RoomId",room1Ref).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                Log.d("KMP", String.valueOf(task.getResult().size()));
                if(task.isSuccessful() && task.getResult().size()>0){
                    temps = new int[task.getResult().size()];
                    Log.d("RRR",String.valueOf(task.getResult().size()));
                    times =new ArrayList<>();
                    int i=0;
                    for(QueryDocumentSnapshot snapshot : task.getResult()) {
                        HashMap<String, Object> val = (HashMap<String, Object>) snapshot.getData();
                        Log.d("CCC",val.get("time").getClass().toString());

                        Date date=null;
                        if(val.get("time").getClass().equals(java.util.Date.class)){
                            date = (Date) val.get("time");
                        }else {
                            com.google.firebase.Timestamp ts= (com.google.firebase.Timestamp) val.get("time");
                            date = ts.toDate();
                        }

                        SimpleDateFormat format = new SimpleDateFormat("hh:mm");

                        Log.d("BBB", format.format(date));
                        times.add(format.format(date));

                        Long x = (Long) val.get("temperature");
                        temps[i] = x.intValue();
                        i++;
                    }
                    buildChart();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("EER",e.toString());
            }
        });
    }
    private DataSet setDataChart(int[] data){
        LineData d = new LineData();
        ArrayList<Entry> entries = new ArrayList<Entry>();

        for (int index = 0; index < data.length; index++) {
            entries.add(new Entry(index, data[index]));
        }
        LineDataSet set = new LineDataSet(entries, "Request Ots approved");
        set.setColor(Color.GREEN);
        set.setLineWidth(2.5f);
        set.setCircleColor(Color.GREEN);
        set.setCircleRadius(5f);
        set.setFillColor(Color.GREEN);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(true);
        set.setValueTextSize(10f);
        set.setValueTextColor(Color.GREEN);

        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        d.addDataSet(set);

        return set;
    }
    void selectDay(){
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month =calendar.get(Calendar.MONTH);
        int date  = calendar.get(Calendar.DATE);
        DatePickerDialog datePickerDialog = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                Calendar cal= Calendar.getInstance();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH,month);
                cal.set(Calendar.DATE,dayOfMonth);
                cal.set(Calendar.HOUR,0);
                cal.set(Calendar.MINUTE,0);
                cal.set(Calendar.SECOND,0);
                cal.set(Calendar.MILLISECOND,0);
                dateMl = cal.getTime().getTime();
                setChart();
            }
        }, year,month,date);
        datePickerDialog.show();
    }
    // quan trong la khi ma change thang kia no kich hoat thang nay :dung roi do : =>
    void selectHour(){
        Calendar calendar = Calendar.getInstance();
        final int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                //Log.e("EEE", "onTimeSet: " + hour + ", " + minute);
                Log.d("VVV", "hour : "+ String.valueOf(hourOfDay)+" and minute : "+ String.valueOf(minute));
                hmMl = hourOfDay*1000*3600 + minute*1000*60;
                setChart();
            }
        }, hour, minute, true);

        timePickerDialog.show();
    }
    @Override
    public void onStart() {
        super.onStart();

    }
}
