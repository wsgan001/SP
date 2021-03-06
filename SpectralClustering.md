# 谱聚类
> 一般来说，谱聚类主要的注意点为相似矩阵的生成方式，切图的方式以及最后的聚类方法。最常用的相似矩阵的生成方式是基于高斯核距离的全连接方式，最常用的切图方式是Ncut。而到最后常用的聚类方法为K-Means。




## Spectral Clustering 算法的全貌：

1）根据数据构造一个 Graph ，Graph 的每一个节点对应一个数据点，将相似的点连接起来，并且边的权重用于表示数据之间的相似度。把这个 Graph 用邻接矩阵的形式表示出来，记为 W 。

2)把每一列元素加起来得到N 个数，把它们放在对角线上（其他地方都是零），组成一个N*N的矩阵，记为D 。并令L = D - W 。

3)求出L的前k个特征值（在本文中，除非特殊说明，否则“前k个”指按照特征值的大小从小到大的顺序）以及对应的特征向量。

4)把这k个特征（列）向量排列在一起组成一个N*k的矩阵，将其中每一行看作k维空间中的一个向量，并使用 K-means 算法进行聚类。聚类的结果中每一行所属的类别就是原来 Graph 中的节点亦即最初的N个数据点分别所属的类别。

## 简单的 Matlab 实现：
    function idx = spectral_clustering(W, k)
        D = diag(sum(W));
        L = D-W;
        opt = struct('issym', true, 'isreal', true);
        [V dummy] = eigs(L, D, k, 'SM', opt);
        idx = kmeans(V, k);
    end




## 下面总结下谱聚类算法的优缺点:

1. 谱聚类算法的主要优点有：
    1. 谱聚类只需要数据之间的相似度矩阵，因此对于处理稀疏数据的聚类很有效。这点传统聚类算法比如K-Means很难做到
    2. 由于使用了降维，因此在处理高维数据聚类时的复杂度比传统聚类算法好。
2. 谱聚类算法的主要缺点有：
    1. 如果最终聚类的维度非常高，则由于降维的幅度不够，谱聚类的运行速度和最后的聚类效果均不好。
    2. 聚类效果**依赖于相似矩阵**，**不同的相似矩阵**得到的最终聚类**效果可能很不同**。


## 改进
 两个提升速度的地方
1.求特征值是最耗时。可以选择高效的幂迭代法，或者雅可比法来提高求特征值的速度。可以明显改善求解速度。

    def power_iteration(A):
        # Ideally choose a random vector
        # To decrease the chance that our vector
        # Is orthogonal to the eigenvector
        b_k = np.random.rand(A.shape[0])

    for _ in range(num_simulations):
        # calculate the matrix-by-vector product Ab
        b_k1 = np.dot(A, b_k)

        # calculate the norm
        b_k1_norm = np.linalg.norm(b_k1)

        # re normalize the vector
        b_k = b_k1 / b_k1_norm

    return b_k

参考：<http://blog.csdn.net/luckisok/article/details/1602266>
    <https://en.wikipedia.org/wiki/Power_iteration>
    
2.最后还是要通过K-means来求解聚类，通过改进K-means算法提高聚类速度。
参考：Geodesic K-means Clustering
    
## 参考
1. Fast Approximate Spectral Clustering
2. A Tutorial on Spectral Clustering

