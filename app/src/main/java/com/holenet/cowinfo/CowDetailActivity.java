package com.holenet.cowinfo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.holenet.cowinfo.item.Cow;
import com.holenet.cowinfo.item.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CowDetailActivity extends AppCompatActivity {
    private ViewPager vPcowDetail;
    private PagerAdapter adapter;

    private List<Cow> cows;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cow_detail);

        Intent intent = getIntent();
        cows = (List<Cow>) intent.getSerializableExtra("cow_list");
        if (cows == null) {
            int cowId = intent.getIntExtra("cow_id", -1);
            if (cowId == -1)
                finish();
            cows = new ArrayList<>();
            cows.add(new Cow(cowId));
        }
        int initPosition = intent.getIntExtra("position", 0);

        adapter = new PagerAdapter(getSupportFragmentManager());
        vPcowDetail = findViewById(R.id.vPcowDetail);
        vPcowDetail.setAdapter(adapter);
        vPcowDetail.setCurrentItem(initPosition);
    }

    public static class CowDetailFragment extends Fragment {
        public static final int REQUEST_UPDATE_COW = 400;
        public static final int REQUEST_CREATE_RECORD = 401;
        public static final int REQUEST_UPDATE_RECORD = 402;
        public static final int MENU_UPDATE_RECORD = 300;
        public static final int MENU_DELETE_RECORD = 301;

        private static final String ARG_COW = "cow";
        private Cow cow;
        private RecordRecyclerAdapter adapter;

        private TextView tVnumber, tVmotherNumber, tVbirthday;

        private GetCowTask getCowTask;

        private OnCowUpdatedListener onCowUpdatedListener;

        public static CowDetailFragment newInstance(Cow cow) {
            CowDetailFragment fragment = new CowDetailFragment();
            Bundle args = new Bundle();
            args.putSerializable(ARG_COW, cow);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setHasOptionsMenu(true);
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_cow_detail, container, false);
            Context context = view.getContext();

            Cow cow = (Cow) getArguments().getSerializable(ARG_COW);

            tVnumber = view.findViewById(R.id.tVnumber);
            tVmotherNumber = view.findViewById(R.id.tVmotherNumber);
            tVbirthday = view.findViewById(R.id.tVbirthday);

            adapter = new RecordRecyclerAdapter(cow.id, new ArrayList<Record>());
            RecyclerView rVrecordList = view.findViewById(R.id.rVrecordList);
            rVrecordList.setLayoutManager(new LinearLayoutManager(context));
            rVrecordList.setAdapter(adapter);

            Button bTeditInfo = view.findViewById(R.id.bTeditInfo);
            bTeditInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(CowDetailFragment.this.getContext(), EditCowActivity.class);
                    intent.putExtra("edit_mode", EditCowActivity.MODE_UPDATE);
                    intent.putExtra("cow", CowDetailFragment.this.cow);
                    startActivityForResult(intent, REQUEST_UPDATE_COW);
                }
            });

            Button bTaddRecord = view.findViewById(R.id.bTaddRecord);
            bTaddRecord.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(CowDetailFragment.this.getContext(), EditRecordActivity.class);
                    intent.putExtra("cow_id", CowDetailFragment.this.cow.id);
                    intent.putExtra("edit_mode", EditRecordActivity.MODE_CREATE);
                    startActivityForResult(intent, REQUEST_CREATE_RECORD);
                }
            });

            if (cow.number == null) {
                this.cow = cow;
                attemptGetCow();
            } else {
                updateInfo(cow);
            }

            return view;
        }

        private void attemptGetCow() {
            if (getCowTask != null) {
                return;
            }

            getCowTask = new GetCowTask(this);
            getCowTask.execute(cow.id);
        }

        private void attemptDeleteCow() {
            Cow cow = this.cow.copy();
            cow.deleted = true;
            UpdateCowTask task = new UpdateCowTask(this);
            task.execute(cow);
        }

        private void attemptRestoreCow() {
            Cow cow = this.cow.copy();
            cow.deleted = false;
            UpdateCowTask task = new UpdateCowTask(this);
            task.execute(cow);
        }

        private void attemptDestroyCow() {
            DestroyCowTask task = new DestroyCowTask(this);
            task.execute(cow.id);
        }

        private void attemptDestroyRecord(Record record) {
            DestroyRecordTask task = new DestroyRecordTask(this);
            task.execute(record.id);
        }

        private void onSuccessDeleteCow() {
            Toast.makeText(getContext(), "삭제된 개체는 '삭제된 개체' 메뉴에서 복원 할 수 있습니다.", Toast.LENGTH_LONG).show();
            getActivity().finish();
        }

        private void onSuccessRestoreCow() {
            Toast.makeText(getContext(), "성공적으로 복원되었습니다.", Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }

        private void onSuccessDestroyCow() {
            Toast.makeText(getContext(), "완전히 삭제되었습니다.", Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }

        private void updateInfo(Cow cow) {
            this.cow = cow;
            if (onCowUpdatedListener != null) {
                onCowUpdatedListener.onCowUpdated(cow);
            }

            tVnumber.setText(cow.number);
            tVmotherNumber.setText(cow.mother_number);
            tVbirthday.setText(cow.getKoreanBirthday());

            adapter.setItems(cow.records);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.menu_cow_detail, menu);
            if (cow != null) {
                menu.findItem(R.id.mIdelete).setVisible(!cow.deleted);
                menu.findItem(R.id.mIrestore).setVisible(cow.deleted);
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            final int id = item.getItemId();
            if (id == R.id.mIrefresh) {
                attemptGetCow();
            } else if (id == R.id.mIdelete) {
                new AlertDialog.Builder(getContext())
                        .setTitle(cow.number)
                        .setMessage("삭제하시겠습니까?")
                        .setPositiveButton("네", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                attemptDeleteCow();
                            }
                        })
                        .setNegativeButton("아니오", null)
                        .show();
            } else if (id == R.id.mIrestore) {
                new AlertDialog.Builder(getContext())
                        .setTitle(cow.number)
                        .setMessage("복원하시겠습니까?")
                        .setPositiveButton("네", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                attemptRestoreCow();
                            }
                        })
                        .setNegativeButton("아니오", null)
                        .show();
            } else if (id == R.id.mIdestroy) {
                new AlertDialog.Builder(getContext())
                        .setTitle(cow.number)
                        .setMessage("완전 삭제하시겠습니까?\n(이 동작은 되돌릴 수 없습니다.)")
                        .setPositiveButton("네", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                attemptDestroyCow();
                            }
                        })
                        .setNegativeButton("아니오", null)
                        .show();
            } else {
                return super.onOptionsItemSelected(item);
            }
            return true;
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            if (item.getGroupId() != cow.id)
                return false;
            final int index = item.getIntent().getIntExtra("index", -1);
            if (index == -1)
                return false;
            final Record record = adapter.getItem(index);
            final int itemId = item.getItemId();
            if (itemId == MENU_UPDATE_RECORD) {
                Intent intent = new Intent(CowDetailFragment.this.getContext(), EditRecordActivity.class);
                intent.putExtra("cow_id", cow.id);
                intent.putExtra("edit_mode", EditRecordActivity.MODE_UPDATE);
                intent.putExtra("record", record);
                startActivityForResult(intent, REQUEST_UPDATE_RECORD);
            } else if (itemId == MENU_DELETE_RECORD) {
                new AlertDialog.Builder(this.getContext())
                        .setTitle(cow.number)
                        .setMessage(String.format("%s\n%s\n%s\n삭제하시겠습니까?", record.getKoreanDay(), record.content, record.etc))
                        .setPositiveButton("네", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                attemptDestroyRecord(record);
                            }
                        })
                        .setNegativeButton("아니오", null)
                        .show();
            } else {
                return false;
            }
            return true;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_CREATE_RECORD ||
                    requestCode == REQUEST_UPDATE_COW ||
                    requestCode == REQUEST_UPDATE_RECORD) {
                if (resultCode == RESULT_OK) {
                    attemptGetCow();
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }

        static class RecordRecyclerAdapter extends RecyclerView.Adapter<RecordRecyclerAdapter.ViewHolder> {
            private final int cowId;
            private final List<Record> records;

            public RecordRecyclerAdapter(int cowId, List<Record> items) {
                this.cowId = cowId;
                records = items;
            }

            @NonNull
            @Override
            public RecordRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_record, parent, false);
                return new ViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull RecordRecyclerAdapter.ViewHolder holder, int position) {
                final Record record = records.get(position);
                holder.tVcontent.setText(record.content);
                holder.tVetc.setText(record.etc);
                holder.tVday.setText(record.getKoreanDay());
            }

            public Record getItem(int index) {
                return records.get(index);
            }

            public void setItems(List<Record> records) {
                this.records.clear();
                this.records.addAll(records);
                notifyDataSetChanged();
            }

            @Override
            public int getItemCount() {
                return records.size();
            }

            public class ViewHolder extends RecyclerView.ViewHolder {
                public final View view;
                final TextView tVcontent, tVetc, tVday;

                ViewHolder(View view) {
                    super(view);
                    this.view = view;
                    view.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                        @Override
                        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                            Intent intent = new Intent();
                            intent.putExtra("index", getAdapterPosition());
                            menu.add(cowId, MENU_UPDATE_RECORD, Menu.NONE, "이력 수정").setIntent(intent);
                            menu.add(cowId, MENU_DELETE_RECORD, Menu.NONE, "이력 삭제").setIntent(intent);
                        }
                    });

                    tVcontent = view.findViewById(R.id.tVcontent);
                    tVetc = view.findViewById(R.id.tVetc);
                    tVday = view.findViewById(R.id.tVday);
                }
            }
        }

        private static class GetCowTask extends NetworkService.Task<CowDetailFragment, Integer, Cow> {
            public GetCowTask(CowDetailFragment holder) {
                super(holder);
            }

            @Override
            protected NetworkService.Result<Cow> request(Integer cowId) {
                return NetworkService.getCow(cowId);
            }

            @Override
            protected void responseInit(boolean isSuccessful) {
                getHolder().getCowTask = null;
            }

            @Override
            protected void responseSuccess(Cow cow) {
                getHolder().updateInfo(cow);
            }

            @Override
            protected void responseFail(Map<String, String> errors) {
                existErrors(errors, getHolder().getContext());
            }
        }

        private static class UpdateCowTask extends NetworkService.Task<CowDetailFragment, Cow, Cow> {
            public UpdateCowTask(CowDetailFragment holder) {
                super(holder);
            }

            @Override
            protected NetworkService.Result<Cow> request(Cow cow) {
                return NetworkService.updateCow(cow);
            }

            @Override
            protected void responseInit(boolean isSuccessful) {}

            @Override
            protected void responseSuccess(Cow cow) {
                if (cow.deleted) {
                    getHolder().onSuccessDeleteCow();
                } else {
                    getHolder().onSuccessRestoreCow();
                }
            }

            @Override
            protected void responseFail(Map<String, String> errors) {
                if (existErrors(errors, getHolder().getContext())) {
                    Toast.makeText(getHolder().getContext(), "실패하였습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        }

        private static class DestroyCowTask extends NetworkService.Task<CowDetailFragment, Integer, Void> {
            public DestroyCowTask(CowDetailFragment holder) {
                super(holder);
            }

            @Override
            protected NetworkService.Result<Void> request(Integer cowId) {
                return NetworkService.destroyCow(cowId);
            }

            @Override
            protected void responseInit(boolean isSuccessful) {}

            @Override
            protected void responseSuccess(Void aVoid) {
                getHolder().onSuccessDestroyCow();
            }

            @Override
            protected void responseFail(Map<String, String> errors) {
                if (existErrors(errors, getHolder().getContext())) {
                    Toast.makeText(getHolder().getContext(), "완전 삭제에 실패하였습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        }

        private static class DestroyRecordTask extends NetworkService.Task<CowDetailFragment, Integer, Void> {
            public DestroyRecordTask(CowDetailFragment holder) {
                super(holder);
            }

            @Override
            protected NetworkService.Result<Void> request(Integer recordId) {
                return NetworkService.destroyRecord(recordId);
            }

            @Override
            protected void responseInit(boolean isSuccessful) {}

            @Override
            protected void responseSuccess(Void aVoid) {
                getHolder().attemptGetCow();
            }

            @Override
            protected void responseFail(Map<String, String> errors) {
                if (existErrors(errors, getHolder().getContext())) {
                    Toast.makeText(getHolder().getContext(), "삭제에 실패하였습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        }

        public interface OnCowUpdatedListener {
            void onCowUpdated(Cow cow);
        }

        public void setOnCowUpdatedListener(OnCowUpdatedListener onCowUpdatedListener) {
            this.onCowUpdatedListener = onCowUpdatedListener;
        }
    }

    public class PagerAdapter extends FragmentStatePagerAdapter {
        PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(final int position) {
            CowDetailFragment fragment = CowDetailFragment.newInstance(cows.get(position));
            fragment.setOnCowUpdatedListener(new CowDetailFragment.OnCowUpdatedListener() {
                @Override
                public void onCowUpdated(Cow cow) {
                    cows.set(position, cow);
                    notifyDataSetChanged();
                }
            });
            return fragment;
        }

        @Override
        public int getCount() {
            return cows.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return cows.get(position).summary;
        }
    }
}
