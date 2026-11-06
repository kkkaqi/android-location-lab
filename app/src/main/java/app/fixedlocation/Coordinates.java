package app.fixedlocation;

public final class Coordinates {
    public final double latitude;
    public final double longitude;

    private Coordinates(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static Coordinates parse(String latitude, String longitude) {
        double lat = number(latitude, "纬度");
        double lon = number(longitude, "经度");
        if (lat < -90 || lat > 90) throw new IllegalArgumentException("纬度必须在 -90 到 90 之间");
        if (lon < -180 || lon > 180) throw new IllegalArgumentException("经度必须在 -180 到 180 之间");
        return new Coordinates(lat, lon);
    }

    private static double number(String text, String label) {
        if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException("请填写" + label);
        try {
            double result = Double.parseDouble(text.trim());
            if (Double.isNaN(result) || Double.isInfinite(result)) throw new NumberFormatException();
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + "需要填写有效数字");
        }
    }
}
