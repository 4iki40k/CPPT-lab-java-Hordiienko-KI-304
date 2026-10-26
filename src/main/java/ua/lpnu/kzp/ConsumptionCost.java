package ua.lpnu.kzp;

/**
 * Незмінна пара споживання та вартості.
 *
 * @param consumption споживання
 * @param cost вартість
 */
public record ConsumptionCost(double consumption, double cost) {
    /** Перевіряє допустимість обох компонентів. */
    public ConsumptionCost {
        if (!Double.isFinite(consumption) || !Double.isFinite(cost)
                || consumption < 0 || cost < 0) {
            throw new IllegalArgumentException(
                    "Споживання та вартість мають бути скінченними й невід’ємними");
        }
    }
}
