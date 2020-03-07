package org.shadowice.flocke.andotp.Helpers.DropboxServices;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;

import org.shadowice.flocke.andotp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.MetadataViewHolder> {

    private final Callback mCallback;
    private List<Metadata> mFiles;

    public FilesAdapter(Callback callback) {
        mCallback = callback;
    }

    public void setFiles(List<Metadata> files) {
        mFiles = Collections.unmodifiableList(new ArrayList<>(files));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MetadataViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_drivefiles, parent, false);
        return new MetadataViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MetadataViewHolder holder, int position) {

        Metadata mItem = mFiles.get(position);

        holder.mTextView.setText(mItem.getName());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mItem instanceof FolderMetadata) {
                    mCallback.onFolderClicked((FolderMetadata) mItem);
                } else if (mItem instanceof FileMetadata) {
                    mCallback.onFileClicked((FileMetadata) mItem);
                }

            }
        });
    }

    @Override
    public int getItemCount() {
        return mFiles == null ? 0 : mFiles.size();
    }

    public interface Callback {
        void onFolderClicked(FolderMetadata folder);

        void onFileClicked(FileMetadata file);
    }

    class MetadataViewHolder extends RecyclerView.ViewHolder {

        private final TextView mTextView;

        MetadataViewHolder(View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.txtFileName);
        }

    }
}
