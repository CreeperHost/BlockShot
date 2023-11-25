package net.creeperhost.blockshot.lib;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.creeperhost.blockshot.WebUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Created by brandon3055 on 14/09/2023
 */
public class HistoryManager {
    private static final Logger LOGGER = LogManager.getLogger();

    public static HistoryManager instance = new HistoryManager();

    private int historyState = 0;
    private boolean dirty = true;
    private boolean downloading = false;
    private boolean downloadError = false;
    private final List<String> deleting = new ArrayList<>();
    private final List<Capture> captureHistory = new ArrayList<>();
    private final List<CompletableFuture<HistoryTask>> activeTasks = new ArrayList<>();

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDownloading() {
        return downloading;
    }

    public boolean isDownloadError() {
        return downloadError;
    }

    public boolean isDeleting(@Nullable Capture capture) {
        return capture == null ? !deleting.isEmpty() : deleting.contains(capture.id());
    }

    public boolean hasNoHistory() {
        return captureHistory.isEmpty() && !isDownloading();
    }

    public List<Capture> getCaptureHistory() {
        return Collections.unmodifiableList(captureHistory);
    }

    /**
     * Returns a simple integer that gets incremented whenever the capture history is updated.
     * Provides an easy way for the gui to monitor for changes.
     */
    public int getState() {
        return historyState;
    }

    public void updateHistory() {
        if (!dirty) return;
        dirty = false;
        downloading = true;
        downloadError = false;
        activeTasks.add(CompletableFuture.supplyAsync(() -> new DownloadTask().runTask()));
    }

    public void deleteCapture(Capture capture) {
        deleting.add(capture.id());
        captureHistory.remove(capture);
        activeTasks.add(CompletableFuture.supplyAsync(() -> new DeleteTask(capture).runTask()));
        historyState++;
    }

    public void tick() {
        if (activeTasks.isEmpty()) return;

        List<CompletableFuture<HistoryTask>> completed = activeTasks.stream().filter(CompletableFuture::isDone).toList();
        for (CompletableFuture<HistoryTask> task : completed) {
            if (!task.isCompletedExceptionally()) {
                try {
                    task.get().finishTask();
                } catch (InterruptedException | ExecutionException e) {
                    markDirty();
                    LOGGER.error("An error occurred while finishing task", e);
                }
            }
        }
        activeTasks.removeAll(completed);
    }

    private abstract static class HistoryTask {
        public abstract HistoryTask runTask();

        public abstract void finishTask();
    }

    private class DownloadTask extends HistoryTask {
        private final List<Capture> captures = new ArrayList<>();
        private boolean errored = false;

        @Override
        public DownloadTask runTask() {
            String rsp = WebUtils.get("https://blockshot.ch/list", null);
            if (!rsp.equals("error")) {
                JsonElement jsonElement = JsonParser.parseString(rsp);
                JsonArray images = jsonElement.getAsJsonArray();
                for (JsonElement obj : images) {
                    captures.add(Capture.fromJson(obj.getAsJsonObject()));
                }
            } else {
                errored = true;
            }
            return this;
        }

        @Override
        public void finishTask() {
            downloadError = errored;
            downloading = false;
            captureHistory.clear();
            captureHistory.addAll(captures);
            captureHistory.sort(Comparator.comparingLong(Capture::created).reversed());
            historyState++;
        }
    }

    private class DeleteTask extends HistoryTask {
        private final Capture capInfo;

        public DeleteTask(Capture capInfo) {
            this.capInfo = capInfo;
        }

        @Override
        public DeleteTask runTask() {
            WebUtils.get("https://blockshot.ch/delete/" + capInfo.id(), null);
            return this;
        }

        @Override
        public void finishTask() {
            captureHistory.remove(capInfo);
            deleting.remove(capInfo.id());
            historyState++;
        }
    }
}
