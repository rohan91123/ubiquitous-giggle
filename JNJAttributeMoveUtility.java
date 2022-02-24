package ext.jnj.util;

import java.util.Locale;

import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.DisplayOperationIdentifier;
import com.ptc.core.meta.common.IdentifierFactory;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.UpdateOperationIdentifier;

import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.fc.collections.WTArrayList;
import wt.fc.collections.WTCollection;
import wt.log4j.LogR;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.part.WTPart;
import wt.part.WTPartMaster;
import wt.pds.StatementSpec;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.services.applicationcontext.implementation.DefaultServiceProvider;
import wt.session.SessionHelper;
import wt.type.TypedUtilityServiceHelper;
import wt.util.WTException;
import wt.vc.Iterated;
import wt.vc.VersionControlHelper;
import wt.vc.wip.WorkInProgressServerHelper;

// Command Line:- windchill ext.jnj.util.JNJAttributeMoveUtility wcadmin wcadmin wt.part.WTPart#com.synthesJNJProductPart jnjPhantom phantom

/**
 * This Class is a custom utility to move SourceAttribute value (IBA/Soft Type)
 * to Standard Attribute which calls updateTargetAttributeValue() method to
 * update TargetAttribute value.
 * 
 * @author Plural Technology Pvt. Ltd.
 * 
 */
public class JNJAttributeMoveUtility implements RemoteAccess {

	private static final String CLASSNAME = JNJAttributeMoveUtility.class.getName();
	private static final Logger logger = LogR.getLogger(CLASSNAME);

	/**
	 * @param Class Name, sourceAttributeName, targetAttributeName
	 * @return
	 * @return ArrayList to display Source and Target Attribute values
	 * @throws WTException
	 * @throws ClassNotFoundException
	 */
	public static void moveAttributeValue(String className, String sourceAttributeName, String targetAttributeName)
			throws WTException, ClassNotFoundException {
		int counter = 0;
		String[] arrOfStr = className.split("#");
		className =arrOfStr[0];
		QuerySpec querySpec = new QuerySpec(Class.forName(className));

		final IdentifierFactory IDENTIFIER_FACTORY = (IdentifierFactory) DefaultServiceProvider
				.getService(IdentifierFactory.class, "logical");
		TypeIdentifier tid = (TypeIdentifier) IDENTIFIER_FACTORY.get(arrOfStr[1]); // Subtype: "com.synthesJNJProductPart"
		Class qc = Class.forName(className);
		int idx = querySpec.addClassList(qc, true);
		SearchCondition sc = TypedUtilityServiceHelper.service.getSearchCondition(tid, true);
		querySpec.appendWhere(sc, new int[] { idx });
		System.out.println(querySpec);
		QueryResult qur = PersistenceHelper.manager.find((StatementSpec) querySpec);
		WTObject obj = null;
		while (qur.hasMoreElements()) {
			obj = (WTObject) qur.nextElement();
			if (obj instanceof WTPartMaster) {
				WTPartMaster partMaster = (WTPartMaster) obj;
				System.out.println("####### Part Master ---> " + partMaster.getDisplayIdentifier());
			}
			if (obj instanceof WTPart) {
				WTPart part = (WTPart) obj;
				Iterated latestPart = VersionControlHelper.service.getLatestIteration(part, false);
				part = (WTPart) latestPart;
				System.out.println("####### Part ---> " + part.getName() + "," + part.getVersionIdentifier().getValue()
						+ "." + part.getIterationIdentifier().getValue());
			}
			updateTargetAttributeValue((WTObject) obj, sourceAttributeName, targetAttributeName); //Calling the method to copy attribute values and store in TargetAttribute.
			
			counter++;
		}
		System.out.println("Successfully copied from SourceAttribute to TargetAttribute...");
		System.out.println(" ");
		System.out.println(" ");
		System.out.println("Total Part Atrributes Updated... ---> "+ counter);
	}

	/**
	 * @param Object
	 * @param sourceAttributeName
	 * @param targetAttributeName
	 * @throws WTException
	 */
	public static void updateTargetAttributeValue(WTObject object, String sourceAttributeName,
			String targetAttributeName) throws WTException {
		try {
			final PersistableAdapter perAdapter = new PersistableAdapter(object, null, SessionHelper.getLocale(),
					new DisplayOperationIdentifier());
			perAdapter.load(sourceAttributeName);
			Object nObj = perAdapter.get(sourceAttributeName);
			System.out.println("Reading SourceAttribute Value....   ");
			System.out.println("SourceAttribute Value --> " + nObj);
			setTargetAttributeValue(object, nObj, targetAttributeName);
			
		} catch (WTException e) {
			logger.error(CLASSNAME + ".readSourceAttributeValue - cannot locate IBA value for attribute: "
					+ sourceAttributeName);
			if (logger.isTraceEnabled()) {
				e.getStackTrace();
			}
		}
		//deleteSourceAttributeValue(object, sourceAttributeName);

	}
	

	/**
	 * Method to Copy AttributeValue from SourceAttribute to TargetAttribute
	 * 
	 * @param object
	 * @param targetAttributeName
	 * @throws WTException
	 */
	private static void setTargetAttributeValue(WTObject object, Object nObj, String targetAttributeName)
			throws WTException {
		PersistableAdapter persistAdapter = new PersistableAdapter(object, null, Locale.US,
				new UpdateOperationIdentifier());
		WTCollection collection = new WTArrayList();
		collection.add(object);
		WorkInProgressServerHelper.putInTxMapForValidateModifiable(collection);
		persistAdapter.load(targetAttributeName);
		persistAdapter.set(targetAttributeName, nObj);
		WTObject part1 = (WTObject) persistAdapter.apply();
		part1 = (WTObject) PersistenceHelper.manager.modify(part1);
		Object nObj1 = persistAdapter.get(targetAttributeName);
		if (logger.isDebugEnabled()) {
			logger.debug("Utility : setTargetAttributeValue() : Successfully copied attribute value"
					+ targetAttributeName + " and its value is " + nObj1);
		}
		System.out.println("Copying AttributeValue to TargetAttribute... ");
		System.out.println("Target AttributeValue ---> " + nObj1);
	}
	
	public static void deleteSourceAttributeValue(WTObject object, String sourceAttributeName) throws WTException {
				final PersistableAdapter pAdapter = new PersistableAdapter(object, null, SessionHelper.getLocale(),
					new DisplayOperationIdentifier());
			pAdapter.load(sourceAttributeName);
			pAdapter.set(sourceAttributeName, "Unddefined");
			WTPart part = (WTPart) pAdapter.apply();
			part = (WTPart) PersistenceHelper.manager.modify(part);
			System.out.println("Deleting SourceAttribute Value....   ");
			System.out.println("SourceAttribute Value --> " + part);
		}

	public static void main(String[] args) {
		RemoteMethodServer rms = RemoteMethodServer.getDefault();
		rms.setUserName(args[0]);
		rms.setPassword(args[1]);
		@SuppressWarnings("rawtypes")
		Class[] classArray = { String.class, String.class, String.class };
		String[] strArray = { args[2], args[3], args[4] };
		try {
			rms.invoke("moveAttributeValue", "ext.jnj.util.JNJAttributeMoveUtility", null, classArray, strArray);
			System.out.println("Check Method Server..");
		} catch (Exception e) {
		}

	}

}
