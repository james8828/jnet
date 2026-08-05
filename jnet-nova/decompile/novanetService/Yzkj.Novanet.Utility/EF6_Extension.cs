using System;
using System.Collections.Generic;
using System.Linq;
using System.Linq.Expressions;

namespace Yzkj.Novanet.Utility;

public static class EF6_Extension
{
	public static Expression<Func<TSource, object>> GetExpression<TSource>(string propertyName)
	{
		ParameterExpression param = Expression.Parameter(typeof(TSource), "x");
		Expression conversion = Expression.Convert(Expression.Property(param, propertyName), typeof(object));
		return Expression.Lambda<Func<TSource, object>>(conversion, new ParameterExpression[1] { param });
	}

	public static Func<TSource, object> GetFunc<TSource>(string propertyName)
	{
		return GetExpression<TSource>(propertyName).Compile();
	}

	public static IOrderedEnumerable<TSource> OrderBy<TSource>(this IEnumerable<TSource> source, string propertyName)
	{
		return source.OrderBy(GetFunc<TSource>(propertyName));
	}

	public static IOrderedQueryable<TSource> OrderBy<TSource>(this IQueryable<TSource> source, string propertyName)
	{
		return source.OrderBy(GetExpression<TSource>(propertyName));
	}
}
