/**
 * 
 */
package dk.alexandra.fresco.lib.list;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.Assert;

import dk.alexandra.fresco.framework.ProtocolFactory;
import dk.alexandra.fresco.framework.ProtocolProducer;
import dk.alexandra.fresco.framework.TestApplication;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThread;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadConfiguration;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadFactory;
import dk.alexandra.fresco.framework.sce.SCE;
import dk.alexandra.fresco.framework.sce.SCEFactory;
import dk.alexandra.fresco.framework.value.OInt;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.compare.ComparisonProtocolFactoryImpl;
import dk.alexandra.fresco.lib.field.integer.BasicNumericFactory;
import dk.alexandra.fresco.lib.helper.builder.ComparisonProtocolBuilder;
import dk.alexandra.fresco.lib.helper.builder.NumericIOBuilder;
import dk.alexandra.fresco.lib.helper.sequential.SequentialProtocolProducer;
import dk.alexandra.fresco.lib.math.PreprocessedNumericBitFactory;
import dk.alexandra.fresco.lib.math.exp.ExpFromOIntFactory;
import dk.alexandra.fresco.lib.math.exp.PreprocessedExpPipeFactory;
import dk.alexandra.fresco.lib.math.inv.LocalInversionFactory;

/**
 * @author mortenvchristiansen
 *
 */
public class SIntListofTuplesTest {

	

	
	SIntListofTuplesTest(){
		set1=new SInt[3][];
		set2=new SInt[3][];	
		
		//build the two SInt sets
		for (int i=0;i<3;i++){
			SInt[] tuple=new SInt[3];
			SInt s=SInt.;
			
			set1[i]=tuple;
		}
		for (int i=2;i<5;i++){
			SInt[] tuple=new SInt[3];
			
			set2[i]=tuple;
		}
	}
	
	private abstract static class ThreadWithFixture extends TestThread {

		protected SCE sce;

		@Override
		public void setUp() throws IOException {
			sce = SCEFactory.getSCEFromConfiguration(conf.sceConf,
					conf.protocolSuiteConf);
		}

	}

	
	public static class VerticalExpansionTest extends TestThreadFactory {
		@Override
		public TestThread next(TestThreadConfiguration conf) {
			return new ThreadWithFixture() {
				@Override
				public void test() throws Exception {
					TestApplication app = new TestApplication() {

						int[][] newData={{0,1,2},{1,2,3},{2,3,4},{3,4,5},{4,5,6}};
						/**
						 * @throws java.lang.Exception
						 */
						SInt[][] set1, set2;
						
						@Override
						public ProtocolProducer prepareApplication(
								ProtocolFactory provider) {
							BasicNumericFactory bnFactory = (BasicNumericFactory) provider;
							LocalInversionFactory localInvFactory = (LocalInversionFactory) provider;
							PreprocessedNumericBitFactory numericBitFactory = (PreprocessedNumericBitFactory) provider;
							ExpFromOIntFactory expFromOIntFactory = (ExpFromOIntFactory) provider;
							PreprocessedExpPipeFactory expFactory = (PreprocessedExpPipeFactory) provider;
							SequentialProtocolProducer seq = new SequentialProtocolProducer();

							ComparisonProtocolFactoryImpl compFactory = new ComparisonProtocolFactoryImpl(
									80, bnFactory, localInvFactory,
									numericBitFactory, expFromOIntFactory,
									expFactory);
							
							NumericIOBuilder ioBuilder = new NumericIOBuilder(bnFactory);
							ComparisonProtocolBuilder compBuilder = new ComparisonProtocolBuilder(compFactory, bnFactory);
							
							SInt x = ioBuilder.input(three, 1);
							SInt y = ioBuilder.input(five, 1);
							seq.append(ioBuilder.getCircuit());
							
							SInt compResult1 = compBuilder.compare(x, y);
							SInt compResult2 = compBuilder.compare(y, x);
							OInt res1 = ioBuilder.output(compResult1);
							OInt res2 = ioBuilder.output(compResult2);
							outputs = new OInt[] {res1, res2};
							
							seq.append(compBuilder.getCircuit());
							seq.append(ioBuilder.getCircuit());
							
							return seq;
						}
					};
					sce.runApplication(app);
					Assert.assertEquals(BigInteger.ONE, app.getOutputs()[0].getValue());
					Assert.assertEquals(BigInteger.ZERO, app.getOutputs()[1].getValue());
				}
			};
		}
	}

}
