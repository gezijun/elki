package experimentalcode.erich.lsh;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * LSH hash function for vector space data. Depending on the choice of random
 * vectors, it can be appropriate for Manhattan and Euclidean distances.
 * 
 * Reference:
 * <p>
 * Locality-sensitive hashing scheme based on p-stable distributions<br />
 * M. Datar and N. Immorlica and P. Indyk and V. S. Mirrokni<br />
 * Proc. 20th annual symposium on Computational geometry<br />
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "M. Datar and N. Immorlica and P. Indyk and V. S. Mirrokni", title = "Locality-sensitive hashing scheme based on p-stable distributions", booktitle = "Proc. 20th annual symposium on Computational geometry", url = "http://dx.doi.org/10.1145/997817.997857")
public class MultipleProjectionsLocalitySensitiveHashFunction implements LocalitySensitiveHashFunction<NumberVector<?>> {
  /**
   * Projection matrix.
   */
  Matrix projection;

  /**
   * Shift offset.
   */
  double[] shift;

  /**
   * Scaling factor.
   */
  double width;

  /**
   * Random numbers for mixing the hash codes of the individual functions
   */
  int[] randoms1;

  /**
   * Constructor.
   * 
   * @param projection Projection vectors
   * @param width Width of bins
   */
  public MultipleProjectionsLocalitySensitiveHashFunction(Matrix projection, double width, RandomFactory random) {
    super();
    this.projection = projection;
    this.width = width;
    // Generate random shifts:
    final int num = projection.getRowDimensionality();
    this.shift = new double[num];
    Random rnd = random.getRandom();
    for (int i = 0; i < num; i++) {
      shift[i] = rnd.nextDouble() * width;
      randoms1[i] = rnd.nextInt(0xFFFFFFFA) + 1;
    }
  }

  @Override
  public int hashObject(NumberVector<?> vec) {
    long t1sum = 0L;
    // Project the vector:
    final Vector proj = projection.times(vec.getColumnVector());
    for (int i = 0; i < shift.length; i++) {
      int ai = (int) Math.floor((proj.doubleValue(i) + shift[i]) / width);
      t1sum += randoms1[i] * (long) ai;
    }
    return fastModPrime(t1sum);
  }

  /**
   * Fast modulo operation for the largest unsigned integer prime.
   * 
   * @param data Long input
   * @return {@code data % (2^32 - 5)}.
   */
  public static int fastModPrime(long data) {
    // Mix high and low 32 bit:
    int high = (int) (data >>> 32);
    // Use fast multiplication with 5 for high:
    int alpha = ((int) data) + (high << 2 + high);
    // Note that in Java, PRIME will be negative.
    if (alpha < 0 && alpha > -5) {
      alpha = alpha + 5;
    }
    return alpha;
  }
}