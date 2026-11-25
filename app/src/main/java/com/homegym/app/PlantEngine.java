package com.homegym.app;

import java.util.ArrayList;
import java.util.List;

/** Luật trồng cây. Java thuần, không phụ thuộc Android. Ngày = LocalDate.toEpochDay(). */
public final class PlantEngine {

    /** {tên thư mục trong assets/plants/, tên hiển thị}. Thêm cây mới: thêm thư mục 01..75.png và 1 dòng ở đây. */
    static final String[][] PLANTS = {
            {"thienDieu", "Cây thiên điểu"},
    };

    static final int SEED = 1, FULL = 54, LAST = 75, HOLD_DAYS = 7;

    int plant = 0;          // vị trí trong PLANTS
    int frame = SEED;       // 1..75
    boolean mature = false; // đã từng tới khung 54 trong vòng này
    int miss = 0;           // số buổi bỏ liên tiếp
    int hold = 0;           // số ngày đã giữ tươi sau khi tới 54
    long start = -1;        // ngày gieo đầu tiên (không bị phạt)
    long resolved = -1;     // ngày cuối đã chốt sổ
    long done = -1;         // ngày gần nhất đã tính buổi tập
    final List<String> collection = new ArrayList<>(); // "idCây:ngàyNhận"

    /** Lịch trong index.html (WK): Thứ 2 đến Thứ 7, Chủ nhật nghỉ. Đổi lịch thì sửa cả ở đây. */
    static boolean isTrainingDay(long day) {
        // 1970-01-01 là Thứ Năm; 0 = Thứ Hai ... 6 = Chủ nhật
        return Math.floorMod(day + 3, 7L) != 6;
    }

    /** Chốt mọi ngày đã qua (trước hôm nay). Gọi bao nhiêu lần cũng cho cùng kết quả. */
    void advance(long today) {
        if (start < 0) {
            start = today;
            resolved = today - 1;
            return;
        }
        for (long d = resolved + 1; d < today; d++) {
            if (d != done) {
                if (isTrainingDay(d)) {
                    if (d != start) missed();
                } else {
                    keptFresh(d); // ngày nghỉ theo lịch vẫn tính là duy trì
                }
            }
            resolved = d;
        }
    }

    /** Hoàn thành buổi tập hôm nay. Tối đa 1 lần mỗi ngày. */
    boolean complete(long today) {
        advance(today);
        if (done >= today) return false;
        done = today;
        miss = 0;
        if (!mature) {
            frame++;
            if (frame >= FULL) {
                frame = FULL;
                mature = true;
                hold = 0;
            }
        } else if (frame > FULL) {
            frame--;                       // hồi phục 1 khung
            if (frame == FULL) hold = 0;   // tươi lại: bắt đầu đếm 7 ngày
        } else {
            keptFresh(today);
        }
        return true;
    }

    private void missed() {
        miss++;
        int step = 1 << Math.min(miss - 1, 10); // 1, 2, 4, 8, 16...
        if (!mature) {
            frame = Math.max(SEED, frame - step);
            return;
        }
        hold = 0;
        frame += step;
        if (frame > LAST) reset();
    }

    private void keptFresh(long day) {
        if (!mature || frame != FULL) return;
        if (++hold >= HOLD_DAYS) {
            collection.add(PLANTS[plant][0] + ":" + day);
            plant = nextPlant();
            reset();
        }
    }

    /** Ưu tiên cây chưa có trong bộ sưu tập; có đủ rồi thì xoay vòng. */
    private int nextPlant() {
        for (int k = 1; k <= PLANTS.length; k++) {
            int i = (plant + k) % PLANTS.length;
            boolean owned = false;
            for (String c : collection) owned |= c.startsWith(PLANTS[i][0] + ":");
            if (!owned) return i;
        }
        return (plant + 1) % PLANTS.length;
    }

    private void reset() {
        frame = SEED;
        mature = false;
        miss = 0;
        hold = 0;
    }

    String statusText(long today) {
        String t;
        if (frame > FULL) t = "Đang héo, tập " + (frame - FULL) + " buổi để tươi lại";
        else if (mature) t = "Tươi tốt, giữ thêm " + (HOLD_DAYS - hold) + " ngày";
        else t = "Đang lớn " + frame + "/" + FULL;
        if (done == today) return "✔ " + t;
        return t;
    }
}
