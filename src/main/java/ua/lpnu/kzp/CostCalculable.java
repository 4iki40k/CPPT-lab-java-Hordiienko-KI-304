package ua.lpnu.kzp;

/** Контракт об'єкта, який уміє обчислювати вартість. */
public interface CostCalculable
{
    /**
     * Обчислює вартість споживання.
     *
     * @return вартість
     */
    double calculateCost();
}
