package RecommendPath;

import Cluster.*;
import GuideDataStructure.Graph;
import GuideDataStructure.Node;
import GuideDataStructure.Path;
import GuideMainCode.Guider;
import Util.Util;
import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by Administrator on 2017/7/7 0007.
 */
public class TT2 {
    public static void main(String[] args) throws Exception {
        ArrayList<ArrayList<Double>> all_difs = new ArrayList<ArrayList<Double>>();
        ArrayList<ArrayList<Double>> all_spars = new ArrayList<>();


        System.out.println("现在开始读取数据");
        long start = System.currentTimeMillis();
        Map<String, Set<Integer>> history = new HashMap<String, Set<Integer>>();
        CsvReader reader = new CsvReader("/Users/ruanwenjun/IdeaProjects/SP/src/csvData/Paths.csv", ',', Charset.forName("GBK"));
        while (reader.readRecord()) {
            String s = reader.getValues()[1];
            String[] ss = s.split(",");
            Set<Integer> integerSet = new HashSet<>();
            for (String sss : ss) {
                integerSet.add(Integer.valueOf(sss));
            }

            history.put(reader.getValues()[0], integerSet);

        }
        reader.close();


        ArrayList<String[]> customerDistributionRaw = new ArrayList<String[]>();
        reader = new CsvReader("/Users/ruanwenjun/IdeaProjects/SP/src/csvData/CustomerDistribution.csv", ',', Charset.forName("GBK"));
        while (reader.readRecord()) {
            customerDistributionRaw.add(reader.getValues());
        }
        reader.close();

        Map<Integer, double[]> customerDistribution = new HashMap<>();
        for (String[] data : customerDistributionRaw) {
            String[] pro = data[1].split(",");
            double[] pros = new double[pro.length];
            for (int i = 0; i < pro.length; i++) {
                pros[i] = Double.valueOf(pro[i]);
            }
            customerDistribution.put(Integer.valueOf(data[0]), pros);
        }

        System.out.println("现在开始运行算法");

        int times = 1;
//        double det = 0.4 / times;
        double det = 0;

        for (int j = 9; j < 10; j++) {
            ArrayList<Double> difs = new ArrayList<Double>();
            ArrayList<Double> spars = new ArrayList<>();
            double threshold = 0;

            //加载历史数据，并且添加要搜寻的数据
            ArrayList<ScDataPoint> dataSet = new ArrayList<ScDataPoint>();
            int count = 0;
            for (HashMap.Entry<String, Set<Integer>> e : history.entrySet()) {
                if (e.getKey().length() != 0)
                    dataSet.add(new ScDataPoint(e.getKey(), "b" + count));
                count++;
            }
            for (int i = 0; i < times; i++) {
                threshold += det;
                System.out.println("聚类进行中");
                long startTime = System.currentTimeMillis();

//            double threshold = 0.35;
                //设置原始数据集,并开始聚类
                SCluster sCluster = new SCluster(dataSet, customerDistribution.size(), threshold);
                spars.add(sCluster.sparRatio);
                //得到所有聚类结果
                ArrayList<ScCluster> clusters = sCluster.getCluster();

                //计算所有簇类的概率分布
                Map<Integer, Map<String, Map<Integer, Double>>> clusterDistributions = getClusterDistributions(customerDistribution, history, clusters, true);

                //计算路径聚类下的平均簇类
                double[] MeanCustomersProducts1 = getMeanCluster(customerDistribution, clusterDistributions);

                //路径簇类和路径聚类下的平均簇类差值，这里不需要配对，因为平均簇类是一样的
//                    computeErrorByMean(clusterDistributions, true, MeanCustomersProducts1);

                //建立ErrorMap用于簇类配对(自身聚类后计算的概率分布和给定的用户的概率分布之差，error最小为一对)
                int errorsMapLength = clusterDistributions.size();
                double[][] errorsMap = createErrorMap(customerDistribution, clusterDistributions, errorsMapLength);
                //根据ErrorMap进行一一配对，打印路径簇类和给定概率簇类的比较
                double clustered = pairClusterByErrormap(errorsMapLength, errorsMap, true);

                //路径聚类下的平均簇类和给定顾客簇类的差值
                double mean = errorMeancustomerAndCustomer(customerDistribution, true, MeanCustomersProducts1);

                double dif = mean - clustered;
                difs.add(dif);

                System.out.println("聚类结束");

                long endTime = System.currentTimeMillis();
                double total = endTime - startTime;
                System.out.println("谱聚类耗时：" + total);

            }
            all_difs.add(difs);
            all_spars.add(spars);


        }


        int all_difs_length = all_difs.size();
        int difs_length = all_difs.get(0).size();
        ArrayList<Double> mean_difs = new ArrayList<Double>();
        ArrayList<Double> mean_spars = new ArrayList<>();
        for (int i = 0; i < difs_length; i++) {
            double sum_dif = 0;
            for (int j = 0; j < all_difs_length; j++) {
                sum_dif += all_difs.get(j).get(i);
            }
            double mean_dif = sum_dif / all_difs_length;
            mean_difs.add(mean_dif);
        }
        for (int i = 0; i < difs_length; i++) {
            double sum_spars = 0;
            for (int j = 0; j < all_difs_length; j++) {
                sum_spars += all_spars.get(j).get(i);
            }
            double mean_spar = sum_spars / all_difs_length;
            mean_spars.add(mean_spar);
        }
        System.out.println("正在写error.csv");
        String errorFilePath = "ErrorVsThreshold.csv";
        Util.createFile(errorFilePath);
        int threshold = 0;
        try {
            // 创建CSV写对象
            CsvWriter csvWriterError = new CsvWriter(errorFilePath, ',', Charset.forName("GBK"));
            for (int i = 0; i < times; i++) {
                threshold += det;
                String[] headers = new String[2];
                headers[0] = String.valueOf(threshold);
                headers[1] = String.valueOf(mean_difs.get(i));
                csvWriterError.writeRecord(headers);
                csvWriterError.flush();
            }
            csvWriterError.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("正在写sparsity.csv");
        String sparsityFilePath = "SparsityVsThreshold.csv";
        Util.createFile(sparsityFilePath);
        double threshold2 = 0;
        try {
            // 创建CSV写对象
            CsvWriter csvWriterSparsity = new CsvWriter(sparsityFilePath, ',', Charset.forName("GBK"));
            for (int i = 0; i < times; i++) {
                threshold2 += det;
                String[] headers = new String[2];
                headers[0] = String.valueOf(threshold2);
                headers[1] = String.valueOf(mean_spars.get(i));
                csvWriterSparsity.writeRecord(headers);
                csvWriterSparsity.flush();
            }
            csvWriterSparsity.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 根据聚类出来的结果，计算平均cluster
     *
     * @param customerDistribution
     * @param clusterDistributions
     * @return
     */
    private static double[] getMeanCluster(Map<Integer, double[]> customerDistribution, Map<Integer, Map<String, Map<Integer, Double>>> clusterDistributions) {
        double[] MeanCustomersProducts = new double[customerDistribution.get(0).length];
        for (int i = 0; i < customerDistribution.size(); i++) {
            Map<String, Map<Integer, Double>> clusterDistributionTemp = clusterDistributions.get(i);
            for (Map.Entry<String, Map<Integer, Double>> e : clusterDistributionTemp.entrySet()) {
                Map<Integer, Double> ee = e.getValue();//e.getKey()拿到的是cluster的名字
                for (int j = 0; j < ee.size(); j++) {
                    MeanCustomersProducts[j] += ee.get(j);
                }
            }

        }
        for (int j = 0; j < customerDistribution.get(0).length; j++) {
            MeanCustomersProducts[j] = MeanCustomersProducts[j] / customerDistribution.size();
        }
        return MeanCustomersProducts;
    }

    /**
     * 已經知道了這個用戶是哪一類的顧客，拿到那一類顧客對所有商品的概率
     * 比较顾客簇类算出的概率和路径簇类算出的概率
     *
     * @param testPathGenerate
     * @param errors
     * @param newCustomer
     * @param productProbability
     */
    private static void Compare(TestPathGenerate testPathGenerate, List<Double> errors, Map.Entry<Integer, Set<Integer>> newCustomer, Map<Integer, Double> productProbability, double[] MeanCustomersProducts1) {
        int kType = newCustomer.getKey();
        double[] probability = testPathGenerate.CustomersProducts.get(kType);
        int count1 = 0;
        int errorNum = 0;
        double sumerror = 0;
        double sumErrorMean = 0;
        double sumErrorPath = 0;
        for (double p : probability) {
            System.out.println("商品：" + count1 + "顾客簇类算出的概率：" + p + " " + "路徑簇类算出的概率：" + productProbability.get(new Integer(count1)));
            if (productProbability.get(new Integer(count1)) != null) {
                sumerror = sumerror + Math.abs(productProbability.get(new Integer(count1)) - p);
                sumErrorMean = sumErrorMean + Math.abs(MeanCustomersProducts1[count1] - p);
                sumErrorPath = sumErrorPath + Math.abs(productProbability.get(new Integer(count1)) - MeanCustomersProducts1[count1]);
                errorNum++;
            }
            count1++;
        }
        System.out.println("errors:");
        errors.add(sumerror / errorNum);
        System.out.println("(单个簇类比较)顾客簇类和路径簇类的平均误差（每个商品）：" + sumerror / errorNum);
        System.out.println("(单个簇类比较)路径聚类下的平均簇类和顾客簇类的平均误差（每个商品）" + sumErrorMean / errorNum);
        System.out.println("(单个簇类比较)路径聚类下的平均簇类和路径簇类的平均误差（每个商品）" + sumErrorPath / errorNum);
    }


    /**
     * 给定概率的顾客簇类和路径聚类下的平均簇类的比较
     *
     * @param customerDistribution
     * @param printOrNot
     */
    private static double errorMeancustomerAndCustomer(Map<Integer, double[]> customerDistribution, boolean printOrNot, double[] MeanCustomersProducts1) {
        List<Double> errListWithMean = new ArrayList<>();
        for (int i = 0; i < customerDistribution.size(); i++) {
            int productNum = MeanCustomersProducts1.length;
            Map<Integer, double[]> CustomersProducts = customerDistribution;
            double[] pros = CustomersProducts.get(i);

            double error = 0;
            for (int k = 0; k < productNum; k++) {
                error += (Math.abs(pros[k] - MeanCustomersProducts1[k]));

            }
            errListWithMean.add(error / productNum);
        }
        double meanError2 = 0;
        if (printOrNot) {
            System.out.println("给定概率的顾客簇类和路径聚类下的平均簇类的比较:");
            for (Double d : errListWithMean) {
                System.out.println(d + " ");
                meanError2 += d;
            }
            System.out.println("平均误差：" + meanError2 + " ");
        }
        return meanError2;
    }

    /**
     * 进行簇类类别和用户类别的配对
     *
     * @param errorsMapLength
     * @param errorsMap
     */
    private static double pairClusterByErrormap(int errorsMapLength, double[][] errorsMap, boolean printOrNot) {
        Map<Integer, Integer> CPpair = new HashMap<Integer, Integer>();
        List<Double> errList = new ArrayList<>();
        List<Integer> PathVisited = new ArrayList<Integer>();
        List<Integer> CustomerVisited = new ArrayList<Integer>();
        for (int i = 0; i < errorsMapLength; i++) {
            double min = Integer.MAX_VALUE;
            int CustomerClusterIndex = 0;
            int PathClusterIndex = 0;
            for (int j = 0; j < errorsMapLength; j++) {

                for (int k = 0; k < errorsMapLength; k++) {
                    if (PathVisited.contains(j) || CustomerVisited.contains(k))
                        continue;
                    if (errorsMap[j][k] < min) {
                        min = errorsMap[j][k];
                        PathClusterIndex = j;
                        CustomerClusterIndex = k;
                    }
                }
            }
            CPpair.put(PathClusterIndex, CustomerClusterIndex);
            PathVisited.add(PathClusterIndex);
            CustomerVisited.add(CustomerClusterIndex);
            errList.add(min);
        }
        double meanError = 0;
        if (printOrNot) {
            System.out.println("配对结果(左边为路径聚合概率分布,右边为给定概率分布):");
            System.out.println(CPpair);
            System.out.println(errList);
            System.out.println("给定概率的顾客簇类和路径簇类的比较");
//            double meanError = 0;
            for (Double d : errList) {
                System.out.println(d + " ");
                meanError += d;
            }
            System.out.println("平均误差：" + meanError + " ");
        }
        return meanError;
    }

    /**
     * 二维误差数组，用于簇类配对
     *
     * @param customerDistribution
     * @param clusterDistributions
     * @param errorsMapLength
     * @return
     */
    private static double[][] createErrorMap(Map<Integer, double[]> customerDistribution, Map<Integer, Map<String, Map<Integer, Double>>> clusterDistributions, int errorsMapLength) {
        double[][] errorsMap = new double[errorsMapLength][errorsMapLength];
        //i ->Path cluster j-> given customer cluster
        for (int i = 0; i < errorsMapLength; i++) {
            for (int j = 0; j < errorsMapLength; j++) {
                double[] distributionByCustomer = customerDistribution.get(j);
                int productNum = distributionByCustomer.length;
                Map<String, Map<Integer, Double>> clusterDistributionTemp = clusterDistributions.get(i);
                double[] distributionByPath = new double[productNum];
                for (Map.Entry<String, Map<Integer, Double>> e : clusterDistributionTemp.entrySet()) {
                    Map<Integer, Double> ee = e.getValue();
                    for (Map.Entry<Integer, Double> eee : ee.entrySet()) {
                        distributionByPath[eee.getKey()] = eee.getValue();
                    }
                }

                double error = 0;
                for (int k = 0; k < productNum; k++) {

                    double pByPath = distributionByPath[k];
                    double pByCustomer = distributionByCustomer[k];
                    error += (Math.abs(pByCustomer - pByPath));

                }
                errorsMap[i][j] = error / productNum;
            }
        }
        return errorsMap;
    }

    /**
     * 路径簇类和路径聚类下的平均簇类差值
     *
     * @param clusterDistributions
     * @return
     */
    private static double computeErrorByMean(Map<Integer, Map<String, Map<Integer, Double>>> clusterDistributions, boolean printOrNot, double[] MeanCustomersProducts1) {
        List<Double> errListWithMean = new ArrayList<>();
        for (int i = 0; i < TestPathGenerate.K; i++) {
            int productNum = MeanCustomersProducts1.length;
            Map<String, Map<Integer, Double>> clusterDistributionTemp = clusterDistributions.get(i);
            double[] distributionByPath = new double[productNum];
            for (Map.Entry<String, Map<Integer, Double>> e : clusterDistributionTemp.entrySet()) {
                Map<Integer, Double> ee = e.getValue();
                for (Map.Entry<Integer, Double> eee : ee.entrySet()) {
                    distributionByPath[eee.getKey()] = eee.getValue();
                }
            }
            double error = 0;
            for (int k = 0; k < productNum; k++) {
                double pByPath = distributionByPath[k];
                double pByCustomer = MeanCustomersProducts1[k];
                error += (Math.abs(pByCustomer - pByPath));

            }
            errListWithMean.add(error / productNum);
        }
        double meanError2 = 0;
        if (printOrNot) {
            System.out.println("路径簇类和路径聚类下的平均簇类比较:");
            meanError2 = 0;
            for (Double d : errListWithMean) {
                System.out.println(d + " ");
                meanError2 += d;
            }
            System.out.println("平均误差：" + meanError2 + " ");
        }
        return meanError2;
    }

    /**
     * 查看聚类结果
     *
     * @param finalHcClusters
     */
    private static void chekCluster(List<HcCluster> finalHcClusters) {
        for (int m = 0; m < finalHcClusters.size(); m++) {
            System.out.println(finalHcClusters.get(m).getClusterName());
            for (HcDataPoint hcDataPoint : finalHcClusters.get(m).getHcDataPoints()) {
                System.out.println(hcDataPoint.getDataPointName() + ":" + hcDataPoint.getData());
            }
            System.out.println();
        }
    }

    /**
     * 得到所有簇类的概率分布
     *
     * @param customerDistribution
     * @param history              //     * @param t
     * @param finalHcClusters
     * @return
     */
    private static Map<Integer, Map<String, Map<Integer, Double>>> getClusterDistributions(Map<Integer, double[]> customerDistribution, Map<String, Set<Integer>> history, List<ScCluster> finalHcClusters, Boolean printOrNot) {
        Map<Integer, Map<String, Map<Integer, Double>>> clusterDistributions = new HashMap<Integer, Map<String, Map<Integer, Double>>>();
        int countCluster = 0;
        for (int m = 0; m < finalHcClusters.size(); m++) {
            if (printOrNot) {
                System.out.println("簇类名字：" + finalHcClusters.get(m).getClusterName());
            }
            Map<String, Map<Integer, Double>> clusterDistribution = new HashMap<String, Map<Integer, Double>>();
            Map<Integer, Double> productNum = getClusterDistribution(history, finalHcClusters.get(m), printOrNot);
            clusterDistribution.put((finalHcClusters.get(m).getClusterName()), productNum);
            //每一个簇类的概率,商品不存在的补0
            for (Map.Entry<String, Map<Integer, Double>> e : clusterDistribution.entrySet()) {
                for (int i = 0; i < customerDistribution.get(0).length; i++) {
                    if (!e.getValue().containsKey(i)) {
                        e.getValue().put(i, 0.0);
                    }
                }
            }
            clusterDistributions.put(countCluster, clusterDistribution);
            countCluster++;
            if (printOrNot) {
                System.out.println();
            }
        }
        return clusterDistributions;
    }

    /**
     * 得到单个簇类的概率分布
     *
     * @param history   //     * @param t
     * @param hcCluster
     * @return
     */

    private static Map<Integer, Double> getClusterDistribution(Map<String, Set<Integer>> history, ScCluster hcCluster, boolean printOrNot) {
        List<ScDataPoint> dps = hcCluster.getScDataPoints();
        //统计每一条路径中所有已购买商品总数
        Map<Integer, Double> productNum = new HashMap();
        int sum = 0;
        for (ScDataPoint scDataPoint : dps) {
//            if (t.getData().equals(hcDataPoint.getData())) {
//                continue;
//            }
            Set<Integer> products = history.get(scDataPoint.getData());
            for (int product : products) {
                if (!productNum.containsKey(product)) {
                    productNum.put(product, 1.0);
                    sum += 1;
                } else {
                    double num = productNum.get(product);
                    productNum.put(product, ++num);
                    sum += 1;
                }
            }
        }
        //计算一个簇类中商品出现频率,計算所有商品出現的總數，頻率除總數可得到和為1的購買概率分佈。
        for (HashMap.Entry<Integer, Double> e : productNum.entrySet()) {
            double a = e.getValue();
            productNum.put(e.getKey(), a / sum);
        }

        if (productNum.size() == 0 || productNum.isEmpty()) {
            return null;
        }
        if (printOrNot) {
            for (HashMap.Entry<Integer, Double> e : productNum.entrySet()) {
                System.out.print("product id:" + e.getKey() + " probability:" + String.format("%4f", e.getValue()) + "   ");
            }
            System.out.println();
        }
        return productNum;
    }
}


