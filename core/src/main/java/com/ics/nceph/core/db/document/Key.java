package com.ics.nceph.core.db.document;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This Class represents a composite primary key for a {@link Document}
 * @author Anshul
 * @since Mar 2, 2023
 * @param <P>
 * @param <S>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDBDocument //Indicates that a class can be serialized as an Amazon DynamoDB document
public class Key<P,S>
{
	@DynamoDBHashKey
	private P partitionKey;

	@DynamoDBRangeKey
	private S sortKey;
}
