package test.demos;

import lombok.Data;
import io.github.swqxdba.smartcopier.SmartCopier;

public class DemoJ {

    static SmartCopier SmartCopier = new SmartCopier();

    public static void main(String[] args) {
        SmartCopier.setDebugMode(true);
        SmartCopier.setDebugOutPutDir("./debug");
        copy(new HotelDto(), new Hotel());

    }

    private static void copy(HotelDto from, Hotel to) {
        SmartCopier.copy(from, to);
    }

    private void copy(Hotel from, Hotel to) {
        to.setHotelName(from.getHotelName());
        to.setHotelCode(from.getHotelCode());
    }

    private void merge(Hotel from, Hotel to) {
        if (to.getHotelName() == null) {
            to.setHotelName(from.getHotelName());
        }
        if (to.getHotelCode() == null) {
            to.setHotelCode(from.getHotelCode());
        }
    }

    private void update(Hotel from, Hotel to) {
        if (from.getHotelName() != null) {
            to.setHotelName(from.getHotelName());
        }
        if (from.getHotelCode() != null) {
            to.setHotelCode(from.getHotelCode());
        }
    }

    @Data
    public static class Hotel {

        String hotelName = "2";

        String hotelCode = "1";
    }

    @Data
    public static class HotelDto {

        String hotelName;

        String hotelCode;
    }
}
