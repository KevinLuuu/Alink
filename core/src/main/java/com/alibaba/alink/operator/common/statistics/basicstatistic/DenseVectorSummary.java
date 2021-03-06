package com.alibaba.alink.operator.common.statistics.basicstatistic;

import org.apache.flink.types.Row;

import com.alibaba.alink.common.linalg.DenseVector;
import com.alibaba.alink.common.linalg.MatVecOp;
import com.alibaba.alink.common.linalg.Vector;
import com.alibaba.alink.common.utils.TableUtil;
import com.alibaba.alink.operator.common.utils.PrettyDisplayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * It is result of DenseVectorSummarizer.
 * You can get vectorSize, mean, variance, and other statistics from this class.
 */
public class DenseVectorSummary extends BaseVectorSummary {

	private static final long serialVersionUID = -4454774580129514139L;
	/**
	 * sum of each feature: sum(x_i)
	 */
	DenseVector sum;

	/**
	 * square sum of each feature: sum(x_i * x_i)
	 */
	DenseVector squareSum;

	/**
	 * min of each feature: min(x_i)
	 */
	DenseVector min;

	/**
	 * max of each feature: max(x_i)
	 */
	DenseVector max;

	/**
	 * l1 norm of each feature: sum(|x_i|)
	 */
	DenseVector normL1;

	/**
	 * It will generated by summary.
	 */
	DenseVectorSummary() {

	}

	@Override
	public String toString() {
		String[] outColNames = new String[] {"count",
			"sum", "mean", "variance", "stdDev", "min", "max", "normL1", "normL2"};

		int vectorSize = this.vectorSize();
		String[] colNames = new String[vectorSize];
		for (int i = 0; i < vectorSize; i++) {
			colNames[i] = String.valueOf(i);
		}

		Object[][] data = new Object[vectorSize][outColNames.length];
		for (int i = 0; i < vectorSize; i++) {
			data[i][0] = count;
			data[i][1] = sum(i);
			data[i][2] = mean(i);
			data[i][3] = variance(i);
			data[i][4] = standardDeviation(i);
			data[i][5] = min(i);
			data[i][6] = max(i);
			data[i][7] = normL1(i);
			data[i][8] = normL2(i);
		}

		return "DenseVectorSummary:" +
			"\n" +
			PrettyDisplayUtils.displayTable(data, vectorSize, outColNames.length, colNames, outColNames, "id", 100, 100);
	}

	/**
	 * vector size.
	 */
	@Override
	public int vectorSize() {
		return sum.size();
	}

	/**
	 * sum of each dimension.
	 */
	@Override
	public Vector sum() {
		return sum;
	}

	/**
	 * mean of each dimension.
	 */
	@Override
	public Vector mean() {
		if (count == 0) {
			return sum;
		} else {
			return sum.scale(1.0 / count);
		}
	}

	/**
	 * variance of each dimension.
	 */
	@Override
	public Vector variance() {
		if (0 == count || 1 == count) {
			return DenseVector.zeros(sum.size());
		} else {
			DenseVector dv = (DenseVector) mean();
			double[] means = dv.getData();
			for (int i = 0; i < means.length; i++) {
				means[i] = Math.max(0.0, (squareSum.get(i) - means[i] * sum.get(i)) / (count - 1));
			}
			return dv;
		}
	}

	/**
	 * standardDeviation of each dimension.
	 */
	@Override
	public Vector standardDeviation() {
		DenseVector dv = (DenseVector) variance();
		double[] vars = dv.getData();
		for (int i = 0; i < vars.length; i++) {
			vars[i] = Math.sqrt(vars[i]);
		}
		return dv;
	}

	/**
	 * min of each dimension.
	 */
	@Override
	public Vector min() {
		return min;
	}

	/**
	 * max of each dimension.
	 */
	@Override
	public Vector max() {
		return max;
	}

	/**
	 * normL1 of each dimension.
	 */
	@Override
	public Vector normL1() {
		return normL1;
	}

	/**
	 * normL2 of each dimension.
	 */
	@Override
	public Vector normL2() {
		DenseVector normL2 = squareSum.clone();
		MatVecOp.apply(normL2, normL2, (Math::sqrt));
		return normL2;
	}

}