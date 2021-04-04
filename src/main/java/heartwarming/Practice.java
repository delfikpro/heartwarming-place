package heartwarming;

import java.util.function.IntToDoubleFunction;

public class Practice {

	public static void main(String[] args) {

		{
			double precision10 = pi(11);
			double precision100 = pi(101);
			double precision1000 = pi(1001);

			System.out.println(evaluate(precision10, 1, Practice::pi));
			System.out.println(evaluate(precision100, 1, Practice::pi));
			System.out.println(evaluate(precision1000, 1, Practice::pi));
		}
		{
			double precision10 = e(11);
			double precision100 = e(101);
			double precision1000 = e(1001);

			System.out.println(evaluate(precision10, 0, Practice::e));
			System.out.println(evaluate(precision100, 0, Practice::e));
			System.out.println(evaluate(precision1000, 0, Practice::e));
		}
		{
			double precision10 = ln2(11);
			double precision100 = ln2(101);
			double precision1000 = ln2(1001);

			System.out.println(evaluate(precision10, 1, Practice::ln2));
			System.out.println(evaluate(precision100, 1, Practice::ln2));
			System.out.println(evaluate(precision1000, 1, Practice::ln2));
		}

	}

	public static double pi(double n) {
		double a = 1 / Math.pow(2, 2 * n - 1) * (1 / (2 * n - 1)) * Math.pow(-1, n + 1);
		double b = 1 / Math.pow(3, 2 * n - 1) * (1 / (2 * n - 1)) * Math.pow(-1, n + 1);
		return 4 * (a + b);
	}

	public static double e(double n) {
		return 1 / factorial(n);
	}

	public static double ln2(double n) {
		return 1 / n * Math.pow(-1, n + 1);
	}

	public static double evaluate(double E, int startN, IntToDoubleFunction function) {
		double s = 0;
		int n = startN;
		while (true) {
			double y = function.applyAsDouble(n);
			if (Math.abs(y) <= E)
				break;
			s += y;
			n++;
		}
		return s;
	}

	public static double factorial(double value) {
		int i = 1;
		double r = 1;
		while (i <= value) {
			r *= i;
			i++;
		}
		return r;
	}

}
